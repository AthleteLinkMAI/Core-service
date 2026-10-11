package team.anonyms.athletelink.coreservice.services.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import team.anonyms.athletelink.coreservice.config.KafkaTopicProperties;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestDto;
import team.anonyms.athletelink.coreservice.entities.OutboxEvent;
import team.anonyms.athletelink.coreservice.repositories.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaOutboxTests {
    @Mock OutboxEventRepository repository;
    @Mock KafkaTopicProperties topics;
    @Mock ObjectMapper objectMapper;
    @Mock KafkaTemplate<UUID, String> kafkaTemplate;
    @InjectMocks KafkaProducerService producer;

    @Test
    void closeCommandIsStoredWithTheRequestAsKafkaKey() throws Exception {
        UUID requestId = UUID.randomUUID();
        CloseRequestDto command = new CloseRequestDto(requestId, 3);
        when(topics.gameCommands()).thenReturn("game.commands");
        when(objectMapper.writeValueAsString(command)).thenReturn("{\"type\":\"CLOSE_REQUESTED\"}");

        producer.enqueueCloseRequest(command);

        var event = org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(event.capture());
        assertNotNull(event.getValue().getId());
        assertEquals(requestId, event.getValue().getAggregateId());
        assertEquals("game.commands", event.getValue().getTopic());
        assertNull(event.getValue().getPublishedAt());
    }

    @Test
    void failedKafkaSendLeavesEventPendingForRetry() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "game.commands", "{}");
        when(repository.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc())
                .thenReturn(Optional.of(event));
        when(kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));
        OutboxDeliveryService delivery = new OutboxDeliveryService(repository, kafkaTemplate);

        assertThrows(IllegalStateException.class, delivery::publishNext);
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

        delivery.publishNext();

        assertNotNull(event.getPublishedAt());
    }
}
