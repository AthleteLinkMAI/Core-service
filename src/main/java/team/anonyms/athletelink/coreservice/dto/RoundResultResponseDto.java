package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Schema(description = "Immutable result recorded for one round of an activity request")
public record RoundResultResponseDto(

        @Schema(description = "Round result UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Activity request UUID", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID requestId,

        @Schema(description = "Number of this round within the request, starting at 1", example = "1")
        Integer roundNumber,

        @Schema(description = "UUIDs of players who won this round")
        List<UUID> winners,

        @Schema(description = "UUIDs of players who lost this round")
        List<UUID> losers,

        @Schema(description = "UUID of the organizer who recorded the result", example = "123e4567-e89b-12d3-a456-426614174002")
        UUID recordedBy,

        @Schema(description = "Time when the result was recorded", example = "2030-06-15T18:30:00Z")
        Instant createdAt
) {}

