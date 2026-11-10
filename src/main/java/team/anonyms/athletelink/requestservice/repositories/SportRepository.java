package team.anonyms.athletelink.requestservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import team.anonyms.athletelink.requestservice.entities.Sport;

public interface SportRepository extends JpaRepository<Sport, String> {}
