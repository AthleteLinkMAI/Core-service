package team.anonyms.athletelink.coreservice.dto;

import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActivityRequestDetailsDto(
        UUID id,
        String title,
        String description,
        String sportName,
        Integer maxPlayers,
        Integer currentPlayers,
        Instant eventDate,
        String addressText,
        Double latitude,
        Double longitude,
        RequestStatus status,
        UUID organizerId,
        List<ParticipantDto> participants
) {}