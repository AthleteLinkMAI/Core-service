package team.anonyms.athletelink.coreservice.dto.kafka;

import team.anonyms.athletelink.coreservice.enums.RoundOutcome;
import java.util.List;
import java.util.UUID;

public record GameRoundResultDto(
        String type,
        UUID eventId,
        UUID requestId,
        Integer roundNumber,
        Long sportId,
        UUID recordedBy,
        List<PlayerResult> players
) {
    public static final String TYPE = "ROUND_RESULT_SUBMITTED";

    public GameRoundResultDto(
            UUID eventId,
            UUID requestId,
            Integer roundNumber,
            Long sportId,
            UUID recordedBy,
            List<PlayerResult> players
    ) {
        this(TYPE, eventId, requestId, roundNumber, sportId, recordedBy, List.copyOf(players));
    }

    public record PlayerResult(UUID userId, RoundOutcome outcome) {}
}

