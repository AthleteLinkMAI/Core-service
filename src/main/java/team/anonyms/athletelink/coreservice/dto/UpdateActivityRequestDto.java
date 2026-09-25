package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Data for updating an existing activity request")
public record UpdateActivityRequestDto(
        
        @Schema(description = "Updated title", example = "Friendly football match - new time")
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100)
        String title,

        @Schema(description = "Updated description", example = "Meeting at the GUK (3rd building)")
        String description,

        @Schema(description = "Updated event date and time (must be in the future)", example = "2030-06-16T19:00:00Z")
        @NotNull
        @Future(message = "Event date must be in the future")
        Instant eventDate,

        @Schema(description = "Updated number of rounds", example = "3")
        @NotNull
        @Min(value = 1, message = "At least 1 round is required")
        @Max(value = 10, message = "Maximum 10 rounds allowed")
        Integer numberOfRounds,

        @Schema(description = "Updated maximum number of players", example = "12")
        @NotNull
        @Min(value = 2, message = "At least 2 players are required")
        Integer maxPlayers
) {}
