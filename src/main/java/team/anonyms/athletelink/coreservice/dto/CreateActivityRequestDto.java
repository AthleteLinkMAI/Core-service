package team.anonyms.athletelink.coreservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

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