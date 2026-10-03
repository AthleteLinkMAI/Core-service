package team.anonyms.athletelink.coreservice.services.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.anonyms.athletelink.coreservice.repositories.OutboxEventRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxDeliveryService {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<UUID, String> kafkaTemplate;

    @Transactional
    public boolean publishNext() {
        var pending = outboxEventRepository.findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc();
        if (pending.isEmpty()) {
            return false;
        }

        var event = pending.get();
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox publication interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Outbox publication failed for event " + event.getId(), ex);
        }
        event.markPublished();
        return true;
    }
}
