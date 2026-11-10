package team.anonyms.athletelink.requestservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Identifier of the newly created planned request")
public record CreateRequestResponseDto(
        @Schema(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID requestId
) {
}
