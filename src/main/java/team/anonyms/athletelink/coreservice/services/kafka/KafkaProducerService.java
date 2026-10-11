package team.anonyms.athletelink.coreservice.services.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.config.KafkaTopicProperties;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestDto;
import team.anonyms.athletelink.coreservice.dto.kafka.GameRoundResultDto;
import team.anonyms.athletelink.coreservice.entities.OutboxEvent;
import team.anonyms.athletelink.coreservice.repositories.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTopicProperties topics;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueRoundResult(GameRoundResultDto event) {
        enqueue(event.eventId(), event.requestId(), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueCloseRequest(CloseRequestDto event) {
        enqueue(UUID.randomUUID(), event.requestId(), event);
    }

    private void enqueue(UUID eventId, UUID requestId, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize Game Service command", ex);
        }
        outboxEventRepository.save(new OutboxEvent(eventId, requestId, topics.gameCommands(), payload));
    }
}
