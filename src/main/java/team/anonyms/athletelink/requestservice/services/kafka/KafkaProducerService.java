package team.anonyms.athletelink.requestservice.services.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.requestservice.config.KafkaTopicProperties;
import team.anonyms.athletelink.requestservice.dto.kafka.ActivityRequestNotificationDto;
import team.anonyms.athletelink.requestservice.dto.kafka.ActivityRequestStartedDto;
import team.anonyms.athletelink.requestservice.dto.kafka.NewParticipantDto;
import team.anonyms.athletelink.requestservice.entities.OutboxEvent;
import team.anonyms.athletelink.requestservice.repositories.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTopicProperties topics;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueJoinRequest(NewParticipantDto event) {
        enqueue(event.eventId(), event.requestId(), topics.gameJoinRequests(), event, "join request");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueStartedRequest(ActivityRequestStartedDto event) {
        enqueueActivityRequest(event.eventId(), event.requestId(), event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueRequestNotification(ActivityRequestNotificationDto event) {
        enqueue(event.eventId(), event.requestId(), topics.requestNotifications(), event, "request notification");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelUnpublishedRequestNotifications(UUID requestId) {
        outboxEventRepository.deleteUnpublishedForRequestAndTopic(requestId, topics.requestNotifications());
    }

    private void enqueueActivityRequest(UUID eventId, UUID requestId, Object event) {
        enqueue(eventId, requestId, topics.activityRequestsChanges(), event, "Game Service command");
    }

    private void enqueue(UUID eventId, UUID requestId, String topic, Object event, String kind) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize " + kind, ex);
        }
        outboxEventRepository.save(new OutboxEvent(eventId, requestId, topic, payload));
    }
}
