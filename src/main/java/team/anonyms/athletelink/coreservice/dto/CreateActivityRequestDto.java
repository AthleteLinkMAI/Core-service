package team.anonyms.athletelink.coreservice.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data for creating new request")
public record CreateActivityRequestDto(
        
        @Schema(description = "Title of the activity", example = "Litrbol in front of the GUK")
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title is too long")
        String title,

        @Schema(description = "Description of the activity", example = "Beer drinking contest")
        String description, 

        @Schema(description = "ID of the sport (from /sports)", example = "2")
        @NotNull(message = "Sport ID is required")
        Long sportId,

        @Schema(description = "Maximum number of players (set up by the organizer)", example = "20")
        @NotNull(message = "Max players count is required")
        Integer maxPlayers,

        @Schema(description = "Event date", example = "2026-12-25T10:00:00Z")
        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        Instant eventDate,

        @Schema(description = "Number of rounds/sets (e.g. 1 for a regular game, 3 for the \"Best of 3\" format)", example = "3")
        @NotNull(message = "Number of rounds is required")
        @Min(value = 1, message = "There must be at least 1 round")
        @Max(value = 10, message = "Maximum 10 rounds allowed")
        Integer numberOfRounds,

        @Schema(description = "Address of the activity", example = "г. Москва, ул. Константина Царева, 12к1")
        @NotBlank(message = "Address is required")
        String addressText,

        @Schema(description = "Latitude of the activity location", example = "55.7558")
        @NotNull(message = "Latitude is required")
        Double latitude,

        @Schema(description = "Longitude of the activity location", example = "37.6173")
        @NotNull(message = "Longitude is required")
        Double longitude
) {}