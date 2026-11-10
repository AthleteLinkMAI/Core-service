package team.anonyms.athletelink.requestservice.dto;

import team.anonyms.athletelink.requestservice.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed information about request room")
public record ActivityRequestDetailsDto(
        
        @Schema(description = "Unique request ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Request title", example = "Friendly football match")
        String title,
        
        @Schema(description = "Description", example = "Outdoor match, bring your own boots")
        String description,
        
        @Schema(description = "Sport name, also used as the sport ID", example = "Футбол")
        String sportName,
        
        @Schema(description = "Maximum number of players", example = "10")
        Integer maxPlayers,
        
        @Schema(description = "Current number of accepted players", example = "4")
        Integer currentPlayers,
        
        @Schema(description = "Event start date and time", example = "2030-06-15T18:00:00Z")
        Instant eventDate,
        
        @Schema(description = "Textual address for display in the card", example = "Москва, ул. Константина Царева, 12к1")
        String addressText,

        @Schema(description = "Planned number of rounds/sets", example = "3")
        Integer numberOfRounds,
        
        @Schema(description = "Latitude (for map marker)", example = "55.715551")
        Double latitude,
        
        @Schema(description = "Longitude (for map marker)", example = "37.554191")
        Double longitude,
        
        @Schema(description = "Current lifecycle status of the request", example = "PLANNED")
        RequestStatus status,

        @Schema(description = "Whether the organizer has registration open; status and capacity also govern joining", example = "false")
        Boolean registrationOpen,
        
        @Schema(description = "UUID of the request organizer", example = "123e4567-e89b-12d3-a456-426614174002")
        UUID organizerId,
        
        @Schema(description = "Stored participant records, including pending and kicked players")
        List<ParticipantDto> participants
) {}
