package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.RequestParticipant;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface RequestParticipantRepository extends JpaRepository<RequestParticipant, UUID> {
    
    Optional<RequestParticipant> findByActivityRequestIdAndUserId(UUID requestId, UUID userId);
    
    long countByActivityRequestIdAndStatus(UUID requestId, ParticipantStatus status);

    List<RequestParticipant> findAllByActivityRequestId(UUID requestId);

    List<RequestParticipant> findAllByActivityRequestIdAndStatus(
            UUID requestId,
            ParticipantStatus status
    );

    void deleteAllByUserId(UUID userId);
}