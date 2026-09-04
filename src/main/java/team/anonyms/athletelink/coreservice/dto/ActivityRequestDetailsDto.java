package team.anonyms.athletelink.coreservice.dto;

import team.anonyms.athletelink.coreservice.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed information about request room")
public record ActivityRequestDetailsDto(
        
        @Schema(description = "Unique request ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Request title", example = "MAI litrbol in GUK")
        String title,
        
        @Schema(description = "Description", example = "30 people for litrbol, bring beer and snacks")
        String description,
        
        @Schema(description = "Sport name", example = "Football")
        String sportName,
        
        @Schema(description = "Maximum number of players", example = "10")
        Integer maxPlayers,
        
        @Schema(description = "Current number of accepted players", example = "4")
        Integer currentPlayers,
        
        @Schema(description = "Event start date and time", example = "2026-09-04T18:00:00Z")
        Instant eventDate,
        
        @Schema(description = "Textual address for display in the card", example = "Москва, ул. Константина Царева, 12к1")
        String addressText,

        @Schema(description = "Planned number of rounds/sets", example = "3")
        Integer numberOfRounds,
        
        @Schema(description = "Latitude (for map marker)", example = "55.715551")
        Double latitude,
        
        @Schema(description = "Longitude (for map marker)", example = "37.554191")
        Double longitude,
        
        @Schema(description = "Current status of the request")
        RequestStatus status,
        
        @Schema(description = "ID of request creator (organizer)")
        UUID organizerId,
        
        @Schema(description = "List of all participants in the room")
        List<ParticipantDto> participants
) {}