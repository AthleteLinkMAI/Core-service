package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import java.util.UUID;

public interface RequestParticipantRepository extends JpaRepository<RequestParticipant, UUID> {}