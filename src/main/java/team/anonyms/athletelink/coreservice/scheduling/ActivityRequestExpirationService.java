package team.anonyms.athletelink.coreservice.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.dto.kafka.ActivityRequestNotificationDto;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;
import team.anonyms.athletelink.coreservice.services.kafka.KafkaProducerService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityRequestExpirationService {
    private final ActivityRequestRepository activityRequestRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    public void processDueRequest(UUID requestId, Instant now) {
        var found = activityRequestRepository.findByIdForUpdate(requestId);
        if (found.isEmpty()) {
            return;
        }
        ActivityRequest request = found.get();
        if (request.getStatus() != RequestStatus.PLANNED) {
            return;
        }

        Instant eventDate = request.getEventDate();
        if (!eventDate.plus(Duration.ofDays(2)).isAfter(now)) {
            kafkaProducerService.cancelUnpublishedRequestNotifications(requestId);
            enqueue(request, ActivityRequestNotificationDto.EXPIRED);
            activityRequestRepository.delete(request);
            return;
        }

        if (!eventDate.plus(Duration.ofHours(1)).isAfter(now)
                && request.getFirstReminderEnqueuedAt() == null) {
            enqueue(request, ActivityRequestNotificationDto.ONE_HOUR_OVERDUE);
            request.setFirstReminderEnqueuedAt(now);
        }
        if (!eventDate.plus(Duration.ofDays(1)).isAfter(now)
                && request.getSecondReminderEnqueuedAt() == null) {
            enqueue(request, ActivityRequestNotificationDto.ONE_DAY_OVERDUE);
            request.setSecondReminderEnqueuedAt(now);
        }
    }

    private void enqueue(ActivityRequest request, String type) {
        kafkaProducerService.enqueueRequestNotification(new ActivityRequestNotificationDto(
                type,
                UUID.randomUUID(),
                request.getId(),
                request.getOrganizerId(),
                request.getTitle(),
                request.getEventDate()));
    }
}
