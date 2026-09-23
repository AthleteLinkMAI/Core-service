package team.anonyms.athletelink.coreservice.controllers;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.ActivityRequestFeedDto;
import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.dto.UpdateActivityRequestDto;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;

@RestController
@RequestMapping("/requests")
@Tag(name = "Game requests", description = "API for creating and managing game requests")
public class ActivityRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRequestController.class);

    private final ActivityRequestService activityRequestService;

    public ActivityRequestController(ActivityRequestService activityRequestService) {
        this.activityRequestService = activityRequestService;
    }

    @PostMapping
    @Operation(
        summary = "Create new request",
        description = "Creates a room for the game. The organizer automatically becomes the first participant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., invalid player limit or empty title)")
    })
    public ResponseEntity<Map<String, UUID>> createRequest(
            @RequestBody @Valid CreateActivityRequestDto dto,
            @Parameter(description = "User ID from API Gateway", hidden = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("User {} is creating a new activity request: {}", userId, dto.title());

        UUID createdRequestId = activityRequestService.createRequest(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("requestId", createdRequestId));
    }

    @PostMapping("/{id}/join")
    @Operation(
        summary = "Join request",
        description = "Adds user to the list of participants if there are available spots."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined"),
            @ApiResponse(responseCode = "400", description = "Request is full, has already started, or user was previously kicked"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<Void> joinRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("User {} attempting to join request {}", userId, requestId);
        activityRequestService.joinRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/leave")
    @Operation(
        summary = "Leave request", 
        description = "Removes user from the request, freeing the spot."
)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player successfully left the request"),
            @ApiResponse(responseCode = "400", description = "User is not a participant of the request or is the organizer"),
            @ApiResponse(responseCode = "404", description = "Request/user not found")
    })
    public ResponseEntity<Void> leaveRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("User {} attempting to leave request {}", userId, requestId);
        activityRequestService.leaveRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel activity request",
            description = "Cancels the entire activity request. Can only be performed by the request organizer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "User is not the organizer, or request is already completed/cancelled")
    })
    public ResponseEntity<Void> cancelRequest(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("User {} is attempting to cancel request {}", userId, requestId);
        activityRequestService.cancelRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/kick/{targetUserId}")
    @Operation(
            summary = "Kick a participant",
            description = "Kicks a user from the request room. The kicked user will not be able to rejoin. Can only be performed by the organizer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully kicked"),
            @ApiResponse(responseCode = "400", description = "Target user is not a participant, or initiator is not the organizer")
    })
    public ResponseEntity<Void> kickParticipant(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Parameter(description = "UUID of the user to kick") @PathVariable("targetUserId") UUID targetUserId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID organizerId
    ) {
        logger.info("Organizer {} is attempting to kick user {} from request {}", organizerId, targetUserId, requestId);
        activityRequestService.kickParticipant(requestId, targetUserId, organizerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Edit activity request",
            description = "Updates text fields, date, and limits of the request. Only available to the organizer while the request is PLANNED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation error, not the organizer, or new limit is too low")
    })
    public ResponseEntity<ActivityRequestDetailsDto> updateRequest(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Valid @RequestBody UpdateActivityRequestDto dto,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.info("Organizer {} is updating request {}", userId, requestId);
        
        ActivityRequestDetailsDto updatedRequest = activityRequestService.updateRequest(requestId, userId, dto);
        
        return ResponseEntity.ok(updatedRequest);
    }

    @GetMapping
    @Operation(
            summary = "Geo-search for requests",
            description = "Looks for requests within a specified radius from the given latitude and longitude. Optionally, filter by sport type."
    )
    public ResponseEntity<List<ActivityRequestFeedDto>> searchNearbyRequests(
            @Parameter(description = "Latitude", example = "55.7558") 
            @RequestParam("lat") Double lat,
            
            @Parameter(description = "Longitude", example = "37.6173") 
            @RequestParam("lon") Double lon,
            
            @Parameter(description = "Radius of search in meters", example = "5000") 
            @RequestParam(value = "radius", defaultValue = "5000") Double radius,
            
            @Parameter(description = "Filter by sport type (optional)", example = "1") 
            @RequestParam(value = "sportId", required = false) Long sportId,

            @Parameter(description = "Begin of time window (ISO-8601)", example = "2026-09-10T10:00:00Z") 
            @RequestParam(value = "startDate", required = false) Instant startDate,
            
            @Parameter(description = "End of time window (ISO-8601)", example = "2026-09-12T22:00:00Z") 
            @RequestParam(value = "endDate", required = false) Instant endDate
    ) {
        logger.info("Searching for requests near [{}, {}] within {} meters. Sport filter: {}. Date range: {} to {}", lat, lon, radius, sportId,
                startDate, endDate
        );
        
        List<ActivityRequestFeedDto> feed = activityRequestService
                .searchNearbyRequests(lat, lon, radius, sportId, startDate, endDate);
                
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request details", description = "Retrieves detailed information about a specific activity request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<ActivityRequestDetailsDto> getRequestDetails(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId
    ) {
        logger.info("Fetching details for request {}", requestId);
        ActivityRequestDetailsDto details = activityRequestService.getRequestDetails(requestId);
        return ResponseEntity.ok(details);
    }
}