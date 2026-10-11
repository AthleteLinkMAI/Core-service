package team.anonyms.athletelink.coreservice.dto.kafka;

import java.util.UUID;

/** Game Service verifies that every planned round has a confirmed result. */
public record CloseRequestDto(
        String type,
        UUID requestId,
        Integer expectedRounds
) {
    public static final String TYPE = "CLOSE_REQUESTED";

    public CloseRequestDto(UUID requestId, Integer expectedRounds) {
        this(TYPE, requestId, expectedRounds);
    }
}
