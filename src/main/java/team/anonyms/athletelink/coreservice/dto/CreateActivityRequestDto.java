package team.anonyms.athletelink.coreservice.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data for creating new request")
public record CreateActivityRequestDto(
        
        @Schema(description = "Title of the activity", example = "Friendly football match")
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title is too long")
        String title,

        @Size(max = 500, message = "Description is too long")
        @Schema(description = "Description of the activity", example = "Outdoor match, bring your own boots")
        String description,

        @Schema(description = "ID of the sport (from /sports)", example = "2")
        @NotNull(message = "Sport ID is required")
        Long sportId,

        @Schema(description = "Maximum number of players (set up by the organizer)", example = "20")
        @NotNull(message = "Max players count is required")
        Integer maxPlayers,

        @Schema(description = "Future event date and time", example = "2030-06-15T18:00:00Z")
        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        Instant eventDate,

        @Schema(description = "Number of rounds to record, from 1 to 10", example = "3")
        @NotNull(message = "Number of rounds is required")
        @Min(value = 1, message = "There must be at least 1 round")
        @Max(value = 10, message = "Maximum 10 rounds allowed")
        Integer numberOfRounds,

        @Schema(description = "Address of the activity", example = "г. Москва, ул. Константина Царева, 12к1")
        @NotBlank(message = "Address is required")
        String addressText,

        @Schema(description = "Latitude of the activity location", example = "55.7558")
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        Double latitude,

        @Schema(description = "Longitude of the activity location", example = "37.6173")
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        Double longitude
) {}
