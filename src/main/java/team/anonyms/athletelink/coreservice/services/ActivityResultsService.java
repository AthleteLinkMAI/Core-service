package team.anonyms.athletelink.coreservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.dto.CompletionAcceptedDto;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.RoundSubmissionAcceptedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.services.kafka.KafkaProducerService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityResultsService {
    private final ActivityRequestRepository activityRequestRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final KafkaProducerService kafkaProducerService;

    /** Validates the organizer's submission; Game Service is the owner of stored results. */
    @Transactional
    public RoundSubmissionAcceptedDto recordRoundResult(
            UUID requestId,
            int roundNumber,
            UUID organizerId,
            RecordRoundResultRequestDto dto
    ) {
        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));
        requireOrganizer(request, organizerId);

        if (request.getStatus() != RequestStatus.ACTIVE) {
            throw new IllegalStateException("Cannot submit results for a request that is not active.");
        }
        if (roundNumber < 1 || roundNumber > request.getNumberOfRounds()) {
            throw new IllegalArgumentException("Round number must be between 1 and " + request.getNumberOfRounds());
        }
        if (dto == null || dto.winners() == null || dto.winners().isEmpty()
                || dto.losers() == null || dto.losers().isEmpty()) {
            throw new IllegalArgumentException("Winners and losers must not be empty.");
        }

        Set<UUID> winners = new HashSet<>(dto.winners());
        Set<UUID> losers = new HashSet<>(dto.losers());
        if (winners.contains(null) || losers.contains(null)) {
            throw new IllegalArgumentException("Player IDs must not be null.");
        }
        if (winners.size() != dto.winners().size() || losers.size() != dto.losers().size()) {
            throw new IllegalArgumentException("A player cannot appear twice in one team.");
        }
        if (!Collections.disjoint(winners, losers)) {
            throw new IllegalArgumentException("A player cannot be both winner and loser.");
        }
        if (winners.size() != losers.size()) {
            throw new IllegalArgumentException("Winners and losers count must be equal.");
        }

        Set<UUID> acceptedPlayers = requestParticipantRepository
                .findAllByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED)
                .stream()
                .map(RequestParticipant::getUserId)
                .collect(Collectors.toSet());
        Set<UUID> selectedPlayers = new HashSet<>(winners);
        selectedPlayers.addAll(losers);
        if (!acceptedPlayers.containsAll(selectedPlayers)) {
            throw new IllegalArgumentException("Round contains players who are not accepted participants.");
        }

        List<GameRoundResultDto.PlayerResult> players = new ArrayList<>(selectedPlayers.size());
        dto.winners().forEach(id -> players.add(new GameRoundResultDto.PlayerResult(id, RoundOutcome.WIN)));
        dto.losers().forEach(id -> players.add(new GameRoundResultDto.PlayerResult(id, RoundOutcome.LOSS)));

        UUID eventId = UUID.randomUUID();
        kafkaProducerService.enqueueRoundResult(new GameRoundResultDto(
                eventId, requestId, roundNumber, request.getSport().getId(), organizerId, players));
        return new RoundSubmissionAcceptedDto(eventId, requestId, roundNumber);
    }

    /** Starts an asynchronous closure attempt. Repeated calls while pending return the same attempt. */
    @Transactional
    public CompletionAcceptedDto completeRequest(UUID requestId, UUID organizerId) {
        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));
        requireOrganizer(request, organizerId);

        if (request.getStatus() == RequestStatus.CONFIRMATION) {
            return new CompletionAcceptedDto(requestId, request.getCompletionAttemptId(), RequestStatus.CONFIRMATION);
        }
        if (request.getStatus() != RequestStatus.ACTIVE) {
            throw new IllegalStateException("Only an active request can be completed.");
        }

        UUID attemptId = UUID.randomUUID();
        request.setCompletionAttemptId(attemptId);
        request.setCompletionErrorMessage(null);
        request.setStatus(RequestStatus.CONFIRMATION);
        kafkaProducerService.enqueueCloseRequest(
                new CloseRequestDto(requestId, attemptId, request.getNumberOfRounds()));
        return new CompletionAcceptedDto(requestId, attemptId, RequestStatus.CONFIRMATION);
    }

    /** Applies only the answer for the currently pending attempt. Duplicate and stale replies are harmless. */
    @Transactional
    public void resolveCompletion(CloseRequestResolvedDto reply) {
        if (reply == null || reply.requestId() == null || reply.successful() == null || !CloseRequestResolvedDto.TYPE.equals(reply.type())) {
            throw new IllegalArgumentException("Malformed request completion reply.");
        }
        var request = activityRequestRepository.findByIdForUpdate(reply.requestId());
        if (request.isEmpty()) {
            return; // The request may have been removed after an account deletion.
        }
        ActivityRequest activityRequest = request.get();
        if (activityRequest.getStatus() != RequestStatus.CONFIRMATION) {
            return;
        }

        if (reply.successful()) {
            activityRequest.setStatus(RequestStatus.COMPLETED);
            activityRequest.setCompletionErrorMessage(null);
        } else {
            activityRequest.setStatus(RequestStatus.ACTIVE);
            String message = reply.reason() == null || reply.reason().isBlank()
                    ? "Game Service could not confirm all round results."
                    : reply.reason();
            activityRequest.setCompletionErrorMessage(message.substring(0, Math.min(message.length(), 255)));
        }
    }

    private void requireOrganizer(ActivityRequest request, UUID organizerId) {
        if (!request.getOrganizerId().equals(organizerId)) {
            throw new ForbiddenOperationException("Only the organizer can submit results or complete this request.");
        }
    }
}
