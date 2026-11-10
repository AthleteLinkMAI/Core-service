package team.anonyms.athletelink.requestservice.dto.kafka;

import team.anonyms.athletelink.requestservice.enums.RequestStatus;

import java.util.UUID;

/** Completion notification emitted by Game Service after the final round. */
public record RequestStatusChangedDto(
        String type,
        UUID eventId,
        UUID requestId,
        RequestStatus status
) {
    public static final String TYPE = "REQUEST_STATUS_CHANGED";
}
