package team.anonyms.athletelink.requestservice.services.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.requestservice.dto.kafka.JoinDecisionDto;
import team.anonyms.athletelink.requestservice.dto.kafka.RequestStatusChangedDto;
import team.anonyms.athletelink.requestservice.dto.kafka.UserChange;
import team.anonyms.athletelink.requestservice.dto.kafka.enums.UserChangeType;
import team.anonyms.athletelink.requestservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.requestservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.requestservice.services.ActivityRequestService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    private final ActivityRequestRepository activityRequestRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final ActivityRequestService activityRequestService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.user-changes}",
            groupId = "${spring.application.name}"
    )
    @Transactional
    public void receiveUserChange(String payload) {
        UserChange change = deserialize(payload, UserChange.class);
        if (change.userChangeType() == UserChangeType.DELETE) {
            activityRequestRepository.deleteAllByOrganizerId(change.userId());
            requestParticipantRepository.deleteAllByUserId(change.userId());
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.game-replies}",
            groupId = "${spring.application.name}"
    )
    public void receiveGameReply(String payload) {
        JsonNode envelope = deserialize(payload, JsonNode.class);
        if (envelope == null || !envelope.isObject()) {
            throw new IllegalArgumentException("Invalid Game Service reply envelope.");
        }
        String type = envelope.path("type").asText();
        if (type.isBlank()) {
            throw new IllegalArgumentException("Game Service reply type is required.");
        }
        switch (type) {
            case JoinDecisionDto.TYPE -> activityRequestService.resolveJoinDecision(
                    deserialize(payload, JoinDecisionDto.class));
            case RequestStatusChangedDto.TYPE -> activityRequestService.applyGameStatus(
                    deserialize(payload, RequestStatusChangedDto.class));
            default -> log.atWarn()
                    .setMessage("Ignoring unsupported Game Service event")
                    .addKeyValue("eventType", type)
                    .log();
        }
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception ex) {
            log.atError()
                    .setMessage("Invalid Kafka message")
                    .setCause(ex)
                    .addKeyValue("API endpoint", "N/A")
                    .addKeyValue("source", "kafka.consumer")
                    .addKeyValue("messageType", type.getSimpleName())
                    .log();
            throw new IllegalArgumentException("Invalid Kafka message", ex);
        }
    }
}
