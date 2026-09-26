package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundResult;

import java.util.List;
import java.util.UUID;

public interface ActivityRoundResultRepository
        extends JpaRepository<ActivityRoundResult, UUID> {

    boolean existsByActivityRequestIdAndRoundNumber(
            UUID requestId,
            Integer roundNumber
    );

    long countByActivityRequestId(UUID requestId);

    List<ActivityRoundResult>
    findAllByActivityRequestIdOrderByRoundNumberAsc(UUID requestId);
}
