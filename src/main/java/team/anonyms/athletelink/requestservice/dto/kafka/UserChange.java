package team.anonyms.athletelink.requestservice.dto.kafka;

import team.anonyms.athletelink.requestservice.dto.kafka.enums.UserChangeType;

import java.util.UUID;

public record UserChange(
        UserChangeType userChangeType,
        UUID userId
) {
}
