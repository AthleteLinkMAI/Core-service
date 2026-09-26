package team.anonyms.athletelink.coreservice.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.RoundResultResponseDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundResult;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundPlayerResult;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import team.anonyms.athletelink.coreservice.repositories.ActivityRoundPlayerResultRepository;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.mappers.ActivityResultsMapper;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.ActivityRoundResultRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityResultsService {

    private final ActivityRequestRepository activityRequestRepository;
    private final ActivityRoundResultRepository activityRoundResultRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final ActivityRoundPlayerResultRepository activityRoundPlayerResultRepository;
    private final ActivityResultsMapper resultsMapper;

    public ActivityResultsService(
            ActivityRequestRepository activityRequestRepository,
            ActivityRoundResultRepository activityRoundResultRepository,
            RequestParticipantRepository requestParticipantRepository,
            ActivityRoundPlayerResultRepository activityRoundPlayerResultRepository,
            ActivityResultsMapper resultsMapper
    ) {
        this.activityRequestRepository = activityRequestRepository;
        this.activityRoundResultRepository = activityRoundResultRepository;
        this.requestParticipantRepository = requestParticipantRepository;
        this.activityRoundPlayerResultRepository = activityRoundPlayerResultRepository;
        this.resultsMapper = resultsMapper;
    }

    @Transactional
    public RoundResultResponseDto recordRoundResult(
        UUID requestId,
        int roundNumber,
        UUID organizerId,
        RecordRoundResultRequestDto dto
    ) {
        ActivityRequest request = activityRequestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Activity request not found"));
        
        if (!request.getOrganizerId().equals(organizerId)) {
            throw new ForbiddenOperationException("User is not the organizer of this request.");
        }

        if (request.getStatus() != RequestStatus.ACTIVE) {
            throw new IllegalStateException("Cannot record results for a request that is not active.");
        }

        if (roundNumber < 1 || roundNumber > request.getNumberOfRounds()) {
            throw new IllegalArgumentException(
                    "Round number must be between 1 and "
                            + request.getNumberOfRounds()
            );
        }

        if (activityRoundResultRepository
                .existsByActivityRequestIdAndRoundNumber(
                        requestId, roundNumber
                )) {
            throw new IllegalStateException(
                    "Result for this round has already been recorded."
            );
        }

        if (dto.winners() == null || dto.winners().isEmpty()
                || dto.losers() == null || dto.losers().isEmpty()) {
            throw new IllegalArgumentException(
                    "Winners and losers must not be empty."
            );
        }

        Set<UUID> winners = new HashSet<>(dto.winners());
        Set<UUID> losers = new HashSet<>(dto.losers());

        if (winners.size() != dto.winners().size()) {
            throw new IllegalArgumentException(
                    "Winners contain duplicate players."
            );
        }

        if (losers.size() != dto.losers().size()) {
            throw new IllegalArgumentException(
                    "Losers contain duplicate players."
            );
        }

        if (!Collections.disjoint(winners, losers)) {
            throw new IllegalArgumentException(
                    "A player cannot be both winner and loser."
            );
        }

        if (winners.size() != losers.size()) {
            throw new IllegalArgumentException(
                    "Winners and losers count must be equal."
            );
        }

        Set<UUID> acceptedPlayerIds = requestParticipantRepository
                .findAllByActivityRequestIdAndStatus(
                        requestId,
                        ParticipantStatus.ACCEPTED
                )
                .stream()
                .map(RequestParticipant::getUserId)
                .collect(Collectors.toSet());

        Set<UUID> selectedPlayers = new HashSet<>(winners);
        selectedPlayers.addAll(losers);

        if (!acceptedPlayerIds.containsAll(selectedPlayers)) {
            throw new IllegalArgumentException(
                    "Round contains players who are not accepted participants."
            );
        }

        ActivityRoundResult round = activityRoundResultRepository.saveAndFlush(
                resultsMapper.toRoundResult(request, roundNumber, organizerId));

        List<ActivityRoundPlayerResult> playerResults = new ArrayList<>();

        for (UUID playerId : dto.winners()) {
            playerResults.add(resultsMapper.toPlayerResult(round, playerId, RoundOutcome.WIN));
        }

        for (UUID playerId : dto.losers()) {
            playerResults.add(resultsMapper.toPlayerResult(round, playerId, RoundOutcome.LOSS));
        }

        activityRoundPlayerResultRepository.saveAll(playerResults);

        return resultsMapper.toResponse(round, playerResults);

    }

    @Transactional(readOnly = true)
    public List<RoundResultResponseDto> getRoundResults(UUID requestId) {
        if (!activityRequestRepository.existsById(requestId)) {
            throw new ResourceNotFoundException(
                    "Activity request not found."
            );
        }

        return activityRoundResultRepository
                .findAllByActivityRequestIdOrderByRoundNumberAsc(requestId)
                .stream()
                .map(round -> resultsMapper.toResponse(
                        round,
                        activityRoundPlayerResultRepository
                                .findAllByRoundResultId(round.getId())
                ))
                .toList();
    }

    @Transactional
    public void completeRequest(
            UUID requestId,
            UUID organizerId
    ) {
        ActivityRequest request = activityRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Activity request not found"));

        if (!request.getOrganizerId().equals(organizerId)) {
            throw new ForbiddenOperationException(
                    "Only the organizer can complete this request."
            );
        }

        if (request.getStatus() != RequestStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active request can be completed."
            );
        }

        long completedRounds =
                activityRoundResultRepository
                        .countByActivityRequestId(requestId);

        if (completedRounds != request.getNumberOfRounds()) {
            throw new IllegalStateException(
                    "All rounds must have results before completion."
            );
        }

        request.setStatus(RequestStatus.COMPLETED);
    }
}
