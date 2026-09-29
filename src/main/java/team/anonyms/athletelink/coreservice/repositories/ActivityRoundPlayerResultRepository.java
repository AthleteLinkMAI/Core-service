package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.ActivityRoundPlayerResult;

import java.util.List;
import java.util.UUID;

public interface ActivityRoundPlayerResultRepository
        extends JpaRepository<ActivityRoundPlayerResult, UUID> {

    List<ActivityRoundPlayerResult>
    findAllByRoundResultId(UUID roundResultId);

    void deleteAllByPlayerId(UUID playerId);
}