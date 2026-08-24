package team.anonyms.athletelink.coreservice.services;

import org.springframework.stereotype.Service;
import team.anonyms.athletelink.coreservice.entities.Sport;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;
import team.anonyms.athletelink.coreservice.dto.SportDto;

import java.util.List;

@Service
public class SportService {

    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    public List<SportDto> getAllSports() {
        return sportRepository.findAll().stream()
                .map(sport -> new SportDto(
                        sport.getId(),
                        sport.getName(),
                        sport.getMinPlayers(),
                        sport.getMaxPlayers(),
                        sport.getIconUrl()
                ))
                .toList();
    }
}