package team.anonyms.athletelink.coreservice.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.anonyms.athletelink.coreservice.entities.OutboxEvent;

import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutboxEvent> findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc();

    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.aggregateId = :requestId AND e.topic = :topic AND e.publishedAt IS NULL")
    void deleteUnpublishedForRequestAndTopic(@Param("requestId") UUID requestId, @Param("topic") String topic);
}
