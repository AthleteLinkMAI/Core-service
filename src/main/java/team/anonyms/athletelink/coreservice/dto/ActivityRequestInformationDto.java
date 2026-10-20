package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Summary of a request organized by or involving the selected user")
public record ActivityRequestInformationDto(
        @Schema(description = "Unique request ID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Request title", example = "Friendly football match")
        String title,

        @Schema(description = "Sport name", example = "Football")
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

        @Schema(description = "Current lifecycle status of the request", example = "PLANNED")
        RequestStatus status,

        @Schema(description = "Whether new players can join the request", example = "true")
        Boolean registrationOpen,

        @Schema(description = "Latitude (for map marker)", example = "55.715551")
        Double latitude,

        @Schema(description = "Longitude (for map marker)", example = "37.554191")
        Double longitude
) {}
