package team.anonyms.athletelink.coreservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.services.kafka.KafkaProducerService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityResultsServiceTests {
    @Mock ActivityRequestRepository requestRepository;
    @Mock RequestParticipantRepository participantRepository;
    @Mock KafkaProducerService producer;
    @InjectMocks ActivityResultsService service;

    private final UUID requestId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();

    @Test
    void submitsOneValidatedRoundCommandWithoutStoringResultsInCore() {
        ActivityRequest request = activeRequest();
        UUID winner = UUID.randomUUID();
        UUID loser = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        when(participantRepository.findAllByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(List.of(participant(winner), participant(loser)));

        var accepted = service.recordRoundResult(requestId, 1, organizerId,
                new RecordRoundResultRequestDto(List.of(winner), List.of(loser)));

        ArgumentCaptor<GameRoundResultDto> event = ArgumentCaptor.forClass(GameRoundResultDto.class);
        verify(producer).enqueueRoundResult(event.capture());
        assertEquals(accepted.eventId(), event.getValue().eventId());
        assertEquals(requestId, event.getValue().requestId());
        assertEquals(1, event.getValue().roundNumber());
        assertEquals(7L, event.getValue().sportId());
        assertEquals(organizerId, event.getValue().recordedBy());
        assertEquals(List.of(
                new GameRoundResultDto.PlayerResult(winner, RoundOutcome.WIN),
                new GameRoundResultDto.PlayerResult(loser, RoundOutcome.LOSS)), event.getValue().players());
    }

    @Test
    void rejectsPlayerWhoWasNotAccepted() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        when(participantRepository.findAllByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.recordRoundResult(
                requestId, 1, organizerId,
                new RecordRoundResultRequestDto(List.of(UUID.randomUUID()), List.of(UUID.randomUUID()))));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void completesAfterGameReplyAndDoesNotQueueDuplicateCheck() {
        ActivityRequest request = activeRequest();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        var accepted = service.completeRequest(requestId, organizerId);
        assertEquals(requestId, accepted.requestId());
        assertEquals(RequestStatus.CONFIRMATION, accepted.status());
        assertEquals(RequestStatus.CONFIRMATION, request.getStatus());

        assertEquals(accepted, service.completeRequest(requestId, organizerId));
        verify(producer).enqueueCloseRequest(any());

        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, true, null));
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
    }

    @Test
    void rejectedCompletionReopensRequestWithReason() {
        ActivityRequest request = activeRequest();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        service.completeRequest(requestId, organizerId);

        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, false, "Round 2 is missing"));

        assertEquals(RequestStatus.ACTIVE, request.getStatus());
        assertEquals("Round 2 is missing", request.getCompletionErrorMessage());
    }

    private ActivityRequest activeRequest() {
        return ActivityRequest.builder()
                .id(requestId)
                .organizerId(organizerId)
                .sport(Sport.builder().id(7L).build())
                .numberOfRounds(3)
                .status(RequestStatus.ACTIVE)
                .build();
    }

    private RequestParticipant participant(UUID userId) {
        return RequestParticipant.builder().userId(userId).status(ParticipantStatus.ACCEPTED).build();
    }
}
