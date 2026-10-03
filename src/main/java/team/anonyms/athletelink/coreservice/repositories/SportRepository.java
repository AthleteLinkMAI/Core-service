package team.anonyms.athletelink.coreservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.coreservice.entities.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {}