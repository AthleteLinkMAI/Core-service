package team.anonyms.athletelink.requestservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A pending application to join an activity request. The participant ID identifies this specific application; a later application by the same user gets a new ID.")
public record PendingJoinRequestDto(
        @Schema(description = "ID of the pending participant record to use when deciding", example = "11111111-1111-1111-1111-111111111111")
        UUID participantId,
        @Schema(description = "Activity request ID", example = "22222222-2222-2222-2222-222222222222")
        UUID requestId,
        @Schema(description = "ID of the user asking to join", example = "33333333-3333-3333-3333-333333333333")
        UUID userId
) {}
