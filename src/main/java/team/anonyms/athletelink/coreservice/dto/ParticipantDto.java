package team.anonyms.athletelink.coreservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.coreservice.enums.ParticipantRole;
import team.anonyms.athletelink.coreservice.enums.ParticipantStatus;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Information about a specific participant in an activity request")
public record ParticipantDto(
        
        @Schema(description = "Unique identifier of the user (used for linking to their public profile)", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,    
        
        @Schema(description = "Role of the user in this specific request", example = "PLAYER")
        ParticipantRole role,
        
        @Schema(description = "Current participation status", example = "ACCEPTED")
        ParticipantStatus status,
        
        @Schema(description = "Timestamp when the user joined the request", example = "2026-09-03T12:00:00Z")
        Instant joinedAt
) {}