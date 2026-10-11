package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.coreservice.enums.RequestStatus;

import java.util.UUID;

@Schema(description = "A request completion check is waiting for Game Service verification")
public record CompletionAcceptedDto(
        UUID requestId,
        RequestStatus status
) {}
