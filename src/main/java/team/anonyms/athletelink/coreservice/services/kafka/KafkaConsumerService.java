package team.anonyms.athletelink.coreservice.services.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.dto.kafka.CloseRequestResolvedDto;
import team.anonyms.athletelink.coreservice.dto.kafka.UserChange;
import team.anonyms.athletelink.coreservice.dto.kafka.enums.UserChangeType;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;
import team.anonyms.athletelink.coreservice.services.ActivityResultsService;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    private final ActivityRequestRepository activityRequestRepository;
    private final RequestParticipantRepository requestParticipantRepository;
    private final ActivityResultsService activityResultsService;
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
        CloseRequestResolvedDto reply = deserialize(payload, CloseRequestResolvedDto.class);
        activityResultsService.resolveCompletion(reply);
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
