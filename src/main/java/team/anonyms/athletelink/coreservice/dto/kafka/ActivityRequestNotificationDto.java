package team.anonyms.athletelink.coreservice.dto.kafka;

import java.time.Instant;
import java.util.UUID;

/** Notification Service resolves organizerId to an email address and deduplicates eventId. */
public record ActivityRequestNotificationDto(
        String type,
        UUID eventId,
        UUID requestId,
        UUID organizerId,
        String title,
        Instant eventDate
) {
    public static final String ONE_HOUR_OVERDUE = "REQUEST_START_OVERDUE_1H";
    public static final String ONE_DAY_OVERDUE = "REQUEST_START_OVERDUE_24H";
    public static final String EXPIRED = "REQUEST_EXPIRED";
}
