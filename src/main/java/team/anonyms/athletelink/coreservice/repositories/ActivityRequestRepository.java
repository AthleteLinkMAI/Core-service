package team.anonyms.athletelink.coreservice.dto;

import org.springframework.data.jpa.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.ActivityRequest;
import java.util.UUID;

public interface ActivityRequestRepository extends JpaRepository<ActivityRequest, UUID> {}