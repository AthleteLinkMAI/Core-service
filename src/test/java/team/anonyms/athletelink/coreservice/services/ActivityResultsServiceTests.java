package team.anonyms.athletelink.coreservice.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import team.anonyms.athletelink.coreservice.exceptions.ForbiddenOperationException;
import team.anonyms.athletelink.coreservice.exceptions.ResourceNotFoundException;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.services.kafka.KafkaProducerService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
    void submitsOneValidatedRoundCommand() {
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
        assertNotNull(accepted.eventId());
        assertEquals(requestId, accepted.requestId());
        assertEquals(1, accepted.roundNumber());
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
    void completeRequestQueuesOneCheckAndIsIdempotentWhilePending() {
        ActivityRequest request = activeRequest();
        request.setCompletionErrorMessage("Previous failure");
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        var accepted = service.completeRequest(requestId, organizerId);
        assertEquals(requestId, accepted.requestId());
        assertEquals(RequestStatus.CONFIRMATION, accepted.status());
        assertEquals(RequestStatus.CONFIRMATION, request.getStatus());
        assertNull(request.getCompletionErrorMessage());

        assertEquals(accepted, service.completeRequest(requestId, organizerId));
        ArgumentCaptor<CloseRequestDto> command = ArgumentCaptor.forClass(CloseRequestDto.class);
        verify(producer).enqueueCloseRequest(command.capture());
        assertEquals(CloseRequestDto.TYPE, command.getValue().type());
        assertEquals(requestId, command.getValue().requestId());
        assertEquals(3, command.getValue().expectedRounds());
    }

    @Test
    void rejectedCompletionReopensRequestWithReason() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.CONFIRMATION);
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, false, "Round 2 is missing"));

        assertEquals(RequestStatus.ACTIVE, request.getStatus());
        assertEquals("Round 2 is missing", request.getCompletionErrorMessage());
    }

    @Test
    void successfulCompletionReplyMarksRequestCompletedAndClearsError() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.CONFIRMATION);
        request.setCompletionErrorMessage("Old error");
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, true, null));

        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        assertNull(request.getCompletionErrorMessage());
        verify(producer, never()).enqueueCloseRequest(any());
    }

    @Test
    void recordRoundResultRejectsMissingRequest() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.recordRoundResult(requestId, 1, organizerId, validRound()));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsNonOrganizer() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        assertThrows(ForbiddenOperationException.class,
                () -> service.recordRoundResult(requestId, 1, UUID.randomUUID(), validRound()));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsNonActiveRequest() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.PLANNED);
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        assertThrows(IllegalStateException.class,
                () -> service.recordRoundResult(requestId, 1, organizerId, validRound()));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsRoundOutsideDeclaredRange() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        for (int round : List.of(0, 4)) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordRoundResult(requestId, round, organizerId, validRound()),
                    "round=" + round);
        }
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsMissingTeams() {
        UUID player = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        List<RecordRoundResultRequestDto> invalid = java.util.Arrays.asList(
                null,
                new RecordRoundResultRequestDto(null, List.of(player)),
                new RecordRoundResultRequestDto(List.of(), List.of(player)),
                new RecordRoundResultRequestDto(List.of(player), null),
                new RecordRoundResultRequestDto(List.of(player), List.of()));
        for (RecordRoundResultRequestDto dto : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordRoundResult(requestId, 1, organizerId, dto));
        }
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsNullPlayerOnEitherTeam() {
        UUID player = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        for (RecordRoundResultRequestDto dto : List.of(
                new RecordRoundResultRequestDto(Collections.singletonList(null), List.of(player)),
                new RecordRoundResultRequestDto(List.of(player), Collections.singletonList(null)))) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordRoundResult(requestId, 1, organizerId, dto));
        }
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsDuplicateWithinEitherTeam() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        for (RecordRoundResultRequestDto dto : List.of(
                new RecordRoundResultRequestDto(List.of(first, first), List.of(second, third)),
                new RecordRoundResultRequestDto(List.of(second, third), List.of(first, first)))) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordRoundResult(requestId, 1, organizerId, dto));
        }
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsPlayerOnBothTeams() {
        UUID player = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordRoundResult(requestId, 1, organizerId,
                        new RecordRoundResultRequestDto(List.of(player), List.of(player))));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void recordRoundResultRejectsUnequalTeamSizes() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        assertThrows(IllegalArgumentException.class,
                () -> service.recordRoundResult(requestId, 1, organizerId,
                        new RecordRoundResultRequestDto(List.of(UUID.randomUUID(), UUID.randomUUID()),
                                List.of(UUID.randomUUID()))));
        verify(producer, never()).enqueueRoundResult(any());
    }

    @Test
    void completeRequestRejectsMissingRequest() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.completeRequest(requestId, organizerId));
        verify(producer, never()).enqueueCloseRequest(any());
    }

    @Test
    void completeRequestRejectsNonOrganizer() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        assertThrows(ForbiddenOperationException.class,
                () -> service.completeRequest(requestId, UUID.randomUUID()));
        verify(producer, never()).enqueueCloseRequest(any());
    }

    @Test
    void completeRequestRejectsNonActiveRequest() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.COMPLETED);
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        assertThrows(IllegalStateException.class, () -> service.completeRequest(requestId, organizerId));
        assertEquals(RequestStatus.COMPLETED, request.getStatus());
        verify(producer, never()).enqueueCloseRequest(any());
    }

    @Test
    void recordRoundResultAcceptsFinalRoundAndPreservesPlayerOrder() {
        UUID winner1 = UUID.randomUUID();
        UUID winner2 = UUID.randomUUID();
        UUID loser1 = UUID.randomUUID();
        UUID loser2 = UUID.randomUUID();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(activeRequest()));
        when(participantRepository.findAllByActivityRequestIdAndStatus(requestId, ParticipantStatus.ACCEPTED))
                .thenReturn(List.of(participant(winner1), participant(winner2), participant(loser1), participant(loser2)));

        var accepted = service.recordRoundResult(requestId, 3, organizerId,
                new RecordRoundResultRequestDto(List.of(winner1, winner2), List.of(loser1, loser2)));

        ArgumentCaptor<GameRoundResultDto> event = ArgumentCaptor.forClass(GameRoundResultDto.class);
        verify(producer).enqueueRoundResult(event.capture());
        assertEquals(3, accepted.roundNumber());
        assertEquals(List.of(
                new GameRoundResultDto.PlayerResult(winner1, RoundOutcome.WIN),
                new GameRoundResultDto.PlayerResult(winner2, RoundOutcome.WIN),
                new GameRoundResultDto.PlayerResult(loser1, RoundOutcome.LOSS),
                new GameRoundResultDto.PlayerResult(loser2, RoundOutcome.LOSS)), event.getValue().players());
    }

    @Test
    void resolveCompletionRejectsMalformedReplies() {
        List<CloseRequestResolvedDto> invalid = java.util.Arrays.asList(
                null,
                new CloseRequestResolvedDto(null, CloseRequestResolvedDto.TYPE, true, null),
                new CloseRequestResolvedDto(requestId, CloseRequestResolvedDto.TYPE, null, null),
                new CloseRequestResolvedDto(requestId, "UNEXPECTED_TYPE", true, null));
        for (CloseRequestResolvedDto reply : invalid) {
            assertThrows(IllegalArgumentException.class, () -> service.resolveCompletion(reply));
        }
        org.mockito.Mockito.verifyNoInteractions(requestRepository);
    }

    @Test
    void resolveCompletionIgnoresReplyForDeletedRequest() {
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.resolveCompletion(
                new CloseRequestResolvedDto(requestId, CloseRequestResolvedDto.TYPE, true, null)));
        verify(producer, never()).enqueueCloseRequest(any());
    }

    @Test
    void resolveCompletionIgnoresReplyWhenRequestIsNotPending() {
        ActivityRequest request = activeRequest();
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, true, null));
        assertEquals(RequestStatus.ACTIVE, request.getStatus());
    }

    @Test
    void resolveCompletionUsesFallbackWhenFailureReasonIsMissingOrBlank() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.CONFIRMATION);
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        for (String reason : java.util.Arrays.asList(null, "   ")) {
            request.setStatus(RequestStatus.CONFIRMATION);
            service.resolveCompletion(new CloseRequestResolvedDto(
                    requestId, CloseRequestResolvedDto.TYPE, false, reason));
            assertEquals(RequestStatus.ACTIVE, request.getStatus());
            assertEquals("Game Service could not confirm all round results.", request.getCompletionErrorMessage());
        }
    }

    @Test
    void resolveCompletionTruncatesLongFailureReason() {
        ActivityRequest request = activeRequest();
        request.setStatus(RequestStatus.CONFIRMATION);
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        String reason = "x".repeat(300);
        service.resolveCompletion(new CloseRequestResolvedDto(
                requestId, CloseRequestResolvedDto.TYPE, false, reason));
        assertEquals(RequestStatus.ACTIVE, request.getStatus());
        assertEquals(reason.substring(0, 255), request.getCompletionErrorMessage());
    }

    private RecordRoundResultRequestDto validRound() {
        return new RecordRoundResultRequestDto(List.of(UUID.randomUUID()), List.of(UUID.randomUUID()));
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
