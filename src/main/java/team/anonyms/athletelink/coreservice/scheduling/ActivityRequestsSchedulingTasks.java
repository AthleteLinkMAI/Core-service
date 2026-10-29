package team.anonyms.athletelink.coreservice.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import team.anonyms.athletelink.coreservice.repositories.ActivityRequestRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActivityRequestsSchedulingTasks {
    private static final int BATCH_SIZE = 100;

    private final ActivityRequestRepository activityRequestRepository;
    private final ActivityRequestExpirationService expirationService;

    @Scheduled(fixedDelayString = "${app.requests.expiration.poll-interval-ms:1000}")
    public void processDueRequests() {
        Instant now = Instant.now();
        while (true) {
            List<UUID> ids = activityRequestRepository.findDueRequestIds(
                    RequestStatus.PLANNED,
                    now.minus(Duration.ofHours(1)),
                    now.minus(Duration.ofDays(1)),
                    now.minus(Duration.ofDays(2)),
                    PageRequest.of(0, BATCH_SIZE));
            if (ids.isEmpty()) {
                return;
            }
            for (UUID id : ids) {
                expirationService.processDueRequest(id, now);
            }
        }
    }
}
