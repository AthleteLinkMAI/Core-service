package team.anonyms.athletelink.coreservice.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.anonyms.athletelink.coreservice.dto.kafka.ActivityRequestNotificationDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.services.kafka.KafkaProducerService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityRequestExpirationServiceTests {
    @Mock ActivityRequestRepository repository;
    @Mock KafkaProducerService producer;

    ActivityRequestExpirationService service;
    UUID requestId;
    Instant eventDate;
    ActivityRequest request;

    @BeforeEach
    void setUp() {
        service = new ActivityRequestExpirationService(repository, producer);
        requestId = UUID.randomUUID();
        eventDate = Instant.parse("2026-10-26T10:00:00Z");
        request = ActivityRequest.builder()
                .id(requestId)
                .title("Football")
                .organizerId(UUID.randomUUID())
                .eventDate(eventDate)
                .status(RequestStatus.PLANNED)
                .build();
        lenient().when(repository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
    }

    @Test
    void noReminderBeforeOneHour() {
        service.processDueRequest(requestId, eventDate.plus(Duration.ofHours(1)).minusNanos(1));
        verifyNoInteractions(producer);
        verify(repository, never()).delete(any());
    }

    @Test
    void oneHourReminderIsEnqueuedOnce() {
        Instant now = eventDate.plus(Duration.ofHours(1));
        service.processDueRequest(requestId, now);
        service.processDueRequest(requestId, now.plusSeconds(1));

        ArgumentCaptor<ActivityRequestNotificationDto> events = ArgumentCaptor.forClass(ActivityRequestNotificationDto.class);
        verify(producer).enqueueRequestNotification(events.capture());
        assertEquals(ActivityRequestNotificationDto.ONE_HOUR_OVERDUE, events.getValue().type());
        assertEquals(requestId, events.getValue().requestId());
        assertEquals(request.getOrganizerId(), events.getValue().organizerId());
        assertEquals(eventDate, events.getValue().eventDate());
        assertNotNull(events.getValue().eventId());
        assertEquals(now, request.getFirstReminderEnqueuedAt());
        assertNull(request.getSecondReminderEnqueuedAt());
    }

    @Test
    void secondReminderIsEnqueuedAtOneDay() {
        request.setFirstReminderEnqueuedAt(eventDate.plus(Duration.ofHours(1)));
        Instant now = eventDate.plus(Duration.ofDays(1));

        service.processDueRequest(requestId, now);

        ArgumentCaptor<ActivityRequestNotificationDto> events = ArgumentCaptor.forClass(ActivityRequestNotificationDto.class);
        verify(producer).enqueueRequestNotification(events.capture());
        assertEquals(ActivityRequestNotificationDto.ONE_DAY_OVERDUE, events.getValue().type());
        assertEquals(now, request.getSecondReminderEnqueuedAt());
        verify(repository, never()).delete(any());
    }

    @Test
    void expiryDeletesOnlyPlannedRequestAndQueuesFinalNotice() {
        service.processDueRequest(requestId, eventDate.plus(Duration.ofDays(2)));

        var order = inOrder(producer, repository);
        order.verify(producer).cancelUnpublishedRequestNotifications(requestId);
        ArgumentCaptor<ActivityRequestNotificationDto> events = ArgumentCaptor.forClass(ActivityRequestNotificationDto.class);
        order.verify(producer).enqueueRequestNotification(events.capture());
        order.verify(repository).delete(request);
        assertEquals(ActivityRequestNotificationDto.EXPIRED, events.getValue().type());
        assertNull(request.getFirstReminderEnqueuedAt());
    }

    @Test
    void activeRequestIsLeftAlone() {
        request.setStatus(RequestStatus.ACTIVE);
        service.processDueRequest(requestId, eventDate.plus(Duration.ofDays(3)));
        verifyNoInteractions(producer);
        verify(repository, never()).delete(any());
    }
}
