package team.anonyms.athletelink.coreservice.dto;

public record SportDto(
        Long id,
        String name,
        Integer minPlayers,
        Integer maxPlayers,
        String iconUrl
) {}