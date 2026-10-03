package team.anonyms.athletelink.coreservice.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import team.anonyms.athletelink.coreservice.entities.OutboxEvent;

import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutboxEvent> findFirstByPublishedAtIsNullOrderByCreatedAtAscIdAsc();
}
