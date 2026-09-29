package team.anonyms.athletelink.coreservice.services.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.dto.kafka.UserChange;
import team.anonyms.athletelink.coreservice.dto.kafka.enums.UserChangeType;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.repositories.ActivityRoundPlayerResultRepository;
import team.anonyms.athletelink.coreservice.repositories.ActivityRoundResultRepository;
import team.anonyms.athletelink.coreservice.repositories.RequestParticipantRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings(value = {"unused"})
public class KafkaConsumerService {
    private final ActivityRequestRepository activityRequestRepository;
    private final ActivityRoundPlayerResultRepository activityRoundPlayerResultRepository;
    private final ActivityRoundResultRepository activityRoundResultRepository;
    private final RequestParticipantRepository requestParticipantRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.user-changes}",
            groupId = "${spring.application.name}"
    )
    public void receiveUserChange(ConsumerRecord<UUID, UserChange> record) {
        if (record.value().userChangeType().equals(UserChangeType.DELETE)) {
            onUserDelete(record.value().userId());
        }
    }

    @Transactional
    private void onUserDelete(UUID userId) {
        activityRequestRepository.deleteAllByOrganizerId(userId);
        activityRoundPlayerResultRepository.deleteAllByPlayerId(userId);
        activityRoundResultRepository.deleteAllByRecorderBy(userId);
        requestParticipantRepository.deleteAllByUserId(userId);
    }
}