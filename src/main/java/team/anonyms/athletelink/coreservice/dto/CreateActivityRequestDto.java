package team.anonyms.athletelink.coreservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateActivityRequestDto(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title is too long")
        String title,

        String description, 

        @NotNull(message = "Sport ID is required")
        Long sportId,

        @NotNull(message = "Max players count is required")
        Integer maxPlayers,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        Instant eventDate,

        @NotBlank(message = "Address is required")
        String addressText,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude
) {}