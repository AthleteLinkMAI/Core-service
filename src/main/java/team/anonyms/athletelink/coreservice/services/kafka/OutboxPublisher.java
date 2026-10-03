package team.anonyms.athletelink.coreservice.services.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    private final OutboxDeliveryService deliveryService;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        try {
            for (int published = 0; published < 100 && deliveryService.publishNext(); published++) {
                // Publish in storage order; stop at the first failure.
            }
        } catch (Exception ex) {
            log.warn("Game command publication will be retried", ex);
        }
    }
}
