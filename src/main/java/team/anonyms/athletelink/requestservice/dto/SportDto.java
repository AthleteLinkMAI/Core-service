package team.anonyms.athletelink.requestservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data transfer object representing a sport category available in the system")
public record SportDto(
        
        @Schema(description = "Sport ID; equal to its name", example = "Футбол")
        String id,
        
        @Schema(description = "Display name of the sport; currently also its ID", example = "Футбол")
        String name,
        
        @Schema(description = "Absolute minimum number of players required for a match", example = "10")
        Integer minPlayers,
        
        @Schema(description = "Absolute maximum number of players allowed for a match", example = "22")
        Integer maxPlayers,
        
        @Schema(description = "Optional URL of the sport icon; may be null")
        String iconUrl,

        @Schema(description = "Whether new activity requests may be created for this sport")
        Boolean isPlayable
) {}
