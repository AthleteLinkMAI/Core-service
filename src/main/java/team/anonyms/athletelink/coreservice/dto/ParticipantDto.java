package team.anonyms.athletelink.coreservice.dto;

import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;

import java.time.Instant;
import java.util.UUID;

public record ParticipantDto(
        UUID userId,    
        ParticipantRole role,
        ParticipantStatus status,
        Instant joinedAt
) {}