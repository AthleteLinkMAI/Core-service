package team.anonyms.athletelink.coreservice.dto.kafka;

import java.util.UUID;

/** The outcome of a closure attempt, published by Game Service. */
public record CloseRequestResolvedDto(
        UUID requestId,
        String type,
        Boolean isSuccessful,
        String errorMessage
) {
    public static final String TYPE = "CLOSE_REQUEST_RESOLVED";
}
