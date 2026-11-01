package team.anonyms.athletelink.requestservice.dto.kafka;

import team.anonyms.athletelink.requestservice.enums.ParticipantRole;

import java.util.List;
import java.util.UUID;

/** Immutable request roster sent to Game and Chat when registration ends. */
public record ActivityRequestStartedDto(
        String type,
        UUID eventId,
        UUID requestId,
        String sportId,
        int numberOfRounds,
        UUID organizerId,
        List<Participant> participants
) {
    public static final String TYPE = "ACTIVITY_REQUEST_STARTED";

    public ActivityRequestStartedDto(UUID eventId, UUID requestId, String sportId,
                                     int numberOfRounds, UUID organizerId,
                                     List<Participant> participants) {
        this(TYPE, eventId, requestId, sportId, numberOfRounds, organizerId,
                List.copyOf(participants));
    }

    public record Participant(UUID participantId, UUID userId, ParticipantRole role) {
    }
}
