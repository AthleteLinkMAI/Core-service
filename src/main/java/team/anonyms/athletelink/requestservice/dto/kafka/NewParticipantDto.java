package team.anonyms.athletelink.requestservice.dto.kafka;

import java.util.UUID;

public record NewParticipantDto(
        String type,
        UUID eventId,
        UUID requestId,
        UUID userId,
        UUID organizerId
) {
    public static final String TYPE = "JOIN_REQUESTED";

    public NewParticipantDto(UUID eventId, UUID requestId, UUID userId, UUID organizerId) {
        this(TYPE, eventId, requestId, userId, organizerId);
    }
}
