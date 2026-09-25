package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Accepted participants who won or lost one round. Both lists must be non-empty, disjoint and equal in size.")
public record RecordRoundResultRequestDto(

        @Schema(description = "UUIDs of the round winners; each player may occur only once")
        @NotEmpty(message = "Winners must not be empty")
        List<@NotNull UUID> winners,

        @Schema(description = "UUIDs of the round losers; each player may occur only once")
        @NotEmpty(message = "Losers must not be empty")
        List<@NotNull UUID> losers
) {}
