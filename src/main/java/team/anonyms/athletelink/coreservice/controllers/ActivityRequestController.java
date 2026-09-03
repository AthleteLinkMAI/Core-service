package team.anonyms.athletelink.coreservice.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.coreservice.dto.ActivityRequestDetailsDto;
import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

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
    @Operation(summary = "Create new request", description = "Creates a room for the game. The organizer automatically becomes the first participant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., invalid player limit or empty title)")
    })
    public ResponseEntity<Map<String, UUID>> createRequest(
            @RequestBody @Valid CreateActivityRequestDto dto,
            @Parameter(description = "User ID from API Gateway", hidden = true)
            @RequestHeader("X-User-Id") String userId
    ) {
        logger.info("User {} is creating a new activity request: {}", userId, dto.title());

        UUID createdRequestId = activityRequestService.createRequest(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("requestId", createdRequestId));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join request", description = "Adds user to the list of participants if there are available spots.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined"),
            @ApiResponse(responseCode = "400", description = "Request is full, has already started, or user was previously kicked"),
            @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<Void> joinRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String userId
    ) {
        logger.info("User {} attempting to join request {}", userId, requestId);
        activityRequestService.joinRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave request", description = "Removes user from the request, freeing the spot.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player successfully left the request"),
            @ApiResponse(responseCode = "400", description = "User is not a participant of the request or is the organizer"),
            @ApiResponse(responseCode = "404", description = "Request/user not found")
    })
    public ResponseEntity<Void> leaveRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") String userId
    ) {
        logger.info("User {} attempting to leave request {}", userId, requestId);
        activityRequestService.leaveRequest(requestId, userId);
        return ResponseEntity.ok().build();
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