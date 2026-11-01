package team.anonyms.athletelink.requestservice.services.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import team.anonyms.athletelink.requestservice.config.KafkaTopicProperties;
import team.anonyms.athletelink.requestservice.dto.kafka.ActivityRequestNotificationDto;
import team.anonyms.athletelink.requestservice.dto.kafka.ActivityRequestStartedDto;
import team.anonyms.athletelink.requestservice.dto.kafka.NewParticipantDto;
import team.anonyms.athletelink.requestservice.entities.OutboxEvent;
import team.anonyms.athletelink.requestservice.enums.ParticipantRole;
import team.anonyms.athletelink.requestservice.repositories.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxTests {
    @Mock OutboxEventRepository repository;
    @Mock KafkaTopicProperties topics;
    @Mock ObjectMapper objectMapper;
    @Mock KafkaTemplate<UUID, String> kafkaTemplate;
    @InjectMocks KafkaProducerService producer;

    @Test
    void joinRequestIsStoredWithTheRequestAsKafkaKey() throws Exception {
        UUID requestId = UUID.randomUUID();
        NewParticipantDto command = new NewParticipantDto(UUID.randomUUID(), requestId,
                UUID.randomUUID(), UUID.randomUUID());
        when(topics.gameJoinRequests()).thenReturn("game.join.requests");
        when(objectMapper.writeValueAsString(command)).thenReturn("{\"type\":\"JOIN_REQUESTED\"}");

        producer.enqueueJoinRequest(command);

        var event = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(event.capture());
        assertEquals(command.eventId(), event.getValue().getId());
        assertEquals(requestId, event.getValue().getAggregateId());
        assertEquals("game.join.requests", event.getValue().getTopic());
        assertEquals("{\"type\":\"JOIN_REQUESTED\"}", event.getValue().getPayload());
        assertNotNull(event.getValue().getCreatedAt());
        assertNull(event.getValue().getPublishedAt());
    }

    @Test
    void startSnapshotKeepsItsEventIdAndSerializedPayload() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ActivityRequestStartedDto command = new ActivityRequestStartedDto(eventId, requestId, "Футбол", 3,
                UUID.randomUUID(), List.of(new ActivityRequestStartedDto.Participant(
                UUID.randomUUID(), UUID.randomUUID(), ParticipantRole.ORGANIZER)));
        when(topics.activityRequestsChanges()).thenReturn("activity.requests.changes");
        when(objectMapper.writeValueAsString(command)).thenReturn("{\"type\":\"ACTIVITY_REQUEST_STARTED\"}");

        producer.enqueueStartedRequest(command);

        var saved = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertEquals(eventId, saved.getValue().getId());
        assertEquals(requestId, saved.getValue().getAggregateId());
        assertEquals("activity.requests.changes", saved.getValue().getTopic());
        assertEquals("{\"type\":\"ACTIVITY_REQUEST_STARTED\"}", saved.getValue().getPayload());
        assertNull(saved.getValue().getPublishedAt());
    }

    @Test
    void notificationUsesItsEventIdAndSeparateTopic() throws Exception {
        var event = new ActivityRequestNotificationDto(
                ActivityRequestNotificationDto.ONE_HOUR_OVERDUE,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Football", Instant.parse("2026-10-26T10:00:00Z"));
        when(topics.requestNotifications()).thenReturn("request.notifications");
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"type\":\"REQUEST_START_OVERDUE_1H\"}");

        producer.enqueueRequestNotification(event);

        var saved = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(saved.capture());
        assertEquals(event.eventId(), saved.getValue().getId());
        assertEquals(event.requestId(), saved.getValue().getAggregateId());
        assertEquals("request.notifications", saved.getValue().getTopic());
    }

    @Test
    void serializationFailureDoesNotCreateOutboxEvent() throws Exception {
        NewParticipantDto command = new NewParticipantDto(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        RuntimeException cause = new IllegalArgumentException("Cannot serialize");
        when(objectMapper.writeValueAsString(command)).thenThrow(cause);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> producer.enqueueJoinRequest(command));

        assertEquals("Could not serialize join request", exception.getMessage());
        assertSame(cause, exception.getCause());
        verifyNoInteractions(repository);
    }

    @Test
    void emptyOutboxDoesNotSendAnything() {
        OutboxDeliveryService delivery = new OutboxDeliveryService(repository, kafkaTemplate);

        assertFalse(delivery.publishNext());

        verify(repository).findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void failedKafkaSendLeavesEventPendingForRetry() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "game.join.requests", "{}");
        when(repository.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
        OutboxDeliveryService delivery = new OutboxDeliveryService(repository, kafkaTemplate);

        IllegalStateException exception = assertThrows(IllegalStateException.class, delivery::publishNext);
        assertEquals("Outbox publication failed for event " + event.getId(), exception.getMessage());
        assertNotNull(exception.getCause());
        assertNull(event.getPublishedAt());
    }

    @Test
    void successfulKafkaAckMarksEventPublished() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "game.commands", "{}");
        when(repository.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        OutboxDeliveryService delivery = new OutboxDeliveryService(repository, kafkaTemplate);

        assertTrue(delivery.publishNext());

        assertNotNull(event.getPublishedAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interruptedSendPreservesPendingEventAndInterruptFlag() throws Exception {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "game.commands", "{}");
        CompletableFuture<org.springframework.kafka.support.SendResult<UUID, String>> future =
                org.mockito.Mockito.mock(CompletableFuture.class);
        when(repository.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()))
                .thenReturn(future);
        when(future.get(10, TimeUnit.SECONDS)).thenThrow(new InterruptedException("Interrupted"));
        OutboxDeliveryService delivery = new OutboxDeliveryService(repository, kafkaTemplate);

        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class, delivery::publishNext);
            assertEquals("Outbox publication interrupted", exception.getMessage());
            assertNull(event.getPublishedAt());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
