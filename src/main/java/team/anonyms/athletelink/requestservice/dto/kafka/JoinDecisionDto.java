package team.anonyms.athletelink.requestservice.dto.kafka;

import java.util.UUID;

/** Game Service decision for one join attempt, correlated by eventId. */
public record JoinDecisionDto(
        String type,
        UUID eventId,
        UUID requestId,
        UUID userId,
        Boolean accepted
) {
    public static final String TYPE = "JOIN_REQUEST_RESOLVED";
}
