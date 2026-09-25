package team.anonyms.athletelink.coreservice.events;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record GameStartedEvent(
        UUID requestId,
        Long sportId,
        UUID organizerId,
        Integer numberOfRounds, 
        List participantIds
) {}