package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A round result command was queued for asynchronous processing by Game Service")
public record RoundSubmissionAcceptedDto(
        UUID eventId,
        UUID requestId,
        Integer roundNumber
) {}
