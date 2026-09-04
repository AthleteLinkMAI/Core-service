package team.anonyms.athletelink.coreservice.services;

import org.springframework.stereotype.Service;
import team.anonyms.athletelink.coreservice.repositories.SportRepository;
import team.anonyms.athletelink.coreservice.dto.SportDto;

import java.util.List;

/**
 * Service class for managing sports-related operations, such as retrieving available sports and their details.
 */
@Service
public class SportService {

    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    /**
     * Gets a list of all available sports, including their details such as name, player limits, and icon URL.
     *
     * @return A list of all available sports.
     */
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