package team.anonyms.athletelink.coreservice.dto;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {}