package team.anonyms.athletelink.requestservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import team.anonyms.athletelink.requestservice.enums.ParticipantRole;
import team.anonyms.athletelink.requestservice.enums.ParticipantStatus;

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
        
        @Schema(description = "Time of acceptance; null if this player was never approved", example = "2030-06-15T12:00:00Z", nullable = true)
        Instant joinedAt
) {}
