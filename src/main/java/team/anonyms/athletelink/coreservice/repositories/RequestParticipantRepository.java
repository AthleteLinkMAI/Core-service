package team.anonyms.athletelink.core_service.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.core_service.entities.RequestParticipant;
import java.util.UUID;

public interface RequestParticipantRepository extends JpaRepository<RequestParticipant, UUID> {}