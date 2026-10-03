package team.anonyms.athletelink.coreservice.dto.kafka;

import team.anonyms.athletelink.coreservice.dto.kafka.enums.UserChangeType;

import java.util.UUID;

public record UserChange(
        UserChangeType userChangeType,
        UUID userId
) {
}
