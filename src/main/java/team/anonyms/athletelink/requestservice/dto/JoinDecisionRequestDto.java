package team.anonyms.athletelink.requestservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Organizer's decision for one pending application")
public record JoinDecisionRequestDto(
        @Schema(description = "true accepts the player; false rejects the application and removes its pending record",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "The decision is required")
        Boolean accepted
) {}
