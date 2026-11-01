package team.anonyms.athletelink.requestservice.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.requestservice.dto.*;
import team.anonyms.athletelink.requestservice.enums.RequestStatus;
import team.anonyms.athletelink.requestservice.services.ActivityRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import team.anonyms.athletelink.requestservice.exceptions.GlobalExceptionHandler.ApiError;

import java.util.HashSet;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/requests")
@Tag(name = "Activity requests", description = "Create requests, manage participants and registration, and start matches")
public class ActivityRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRequestController.class);

    private final ActivityRequestService activityRequestService;

    public ActivityRequestController(ActivityRequestService activityRequestService) {
        this.activityRequestService = activityRequestService;
    }

    @PostMapping
    @Operation(
        summary = "Create an activity request",
        description = "Creates a planned request and adds its organizer as the first accepted participant. The player limit must be within the selected sport's bounds."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Request created with its organizer accepted",
                    content = @Content(schema = @Schema(implementation = CreateRequestResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or player limit", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sport not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "New requests cannot be created for this sport", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CreateRequestResponseDto> createRequest(
            @RequestBody @Valid CreateActivityRequestDto dto,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("User is creating an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests")
                .addKeyValue("userId", userId)
                .addKeyValue("sportId", dto.sportId())
                .addKeyValue("title", dto.title())
                .log();

        UUID createdRequestId = activityRequestService.createRequest(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateRequestResponseDto(createdRequestId));
    }

    @PostMapping("/{id}/join")
    @Operation(
        summary = "Request to join",
        description = "Reserves a place in a planned request and sends the organizer's approval request through Game Service. The player remains PENDING until the asynchronous decision arrives. Rejected requests are removed; a player who left must request approval again."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Approval request queued; the player is PENDING, not yet accepted"),
            @ApiResponse(responseCode = "400", description = "Malformed request data", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Registration closed, all places reserved, or player already pending, accepted or kicked", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> joinRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("User is attempting to join an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/join")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.joinRequest(requestId, userId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{id}/leave")
    @Operation(
        summary = "Leave request", 
        description = "Removes an accepted or pending player from a planned request. A later approval for a withdrawn attempt is ignored. Rejoining requires a new approval request; the organizer cannot leave."
)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player left the request"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request or participant not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned or player cannot leave", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> leaveRequest(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("User is attempting to leave an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/leave")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.leaveRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel activity request",
            description = "Only the organizer may cancel a planned request. An active or completed request cannot be cancelled."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request cancelled"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> cancelRequest(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("User is attempting to cancel an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/cancel")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.cancelRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/kick/{targetUserId}")
    @Operation(
            summary = "Kick a participant",
        description = "The organizer removes an accepted or pending player from a planned request. The KICKED record is retained to prevent another join request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Player kicked"),
            @ApiResponse(responseCode = "400", description = "Organizer attempted to kick themselves", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request or player not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned or player was already kicked", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> kickParticipant(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Parameter(description = "UUID of the user to kick") @PathVariable("targetUserId") UUID targetUserId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        logger.atInfo()
                .setMessage("Organizer is attempting to remove a participant from an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/kick/{targetUserId}")
                .addKeyValue("requestId", requestId)
                .addKeyValue("organizerId", organizerId)
                .addKeyValue("targetUserId", targetUserId)
                .log();
        activityRequestService.kickParticipant(requestId, targetUserId, organizerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Edit activity request",
            description = "The organizer updates a planned request. The new capacity must fit accepted and pending players and stay within the sport's limits."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated request details"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or capacity outside sport limits", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned or capacity is below accepted and pending player count", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ActivityRequestDetailsDto> updateRequest(
            @Parameter(description = "UUID of the request") @PathVariable("id") UUID requestId,
            @Valid @RequestBody UpdateActivityRequestDto dto,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("Organizer is updating an activity request")
                .addKeyValue("API endpoint", "PUT /api/v1/requests/{id}")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        
        ActivityRequestDetailsDto updatedRequest = activityRequestService.updateRequest(requestId, userId, dto);
        
        return ResponseEntity.ok(updatedRequest);
    }

    @PostMapping("/{id}/registration/close")
    @Operation(summary = "Close registration", description = "The organizer stops new join requests while the request remains planned. Earlier pending requests can still be approved.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registration closed"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> closeRegistration(
            @Parameter(description = "Request UUID", required = true) @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("Organizer is closing registration for an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/registration/close")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.closeRegistration(requestId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/registration/open")
    @Operation(summary = "Open registration", description = "The organizer reopens player registration for a planned request.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registration opened"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> openRegistration(
            @Parameter(description = "Request UUID", required = true) @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("Organizer is opening registration for an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/registration/open")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.openRegistration(requestId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a request", description = "The organizer starts a planned request once the sport's minimum number of players has been accepted. Registration closes, pending join attempts are removed, and a snapshot of the accepted roster is queued for Game and Chat. A 204 response means the snapshot is durably queued, not yet processed by either consumer.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Request marked ACTIVE; start snapshot queued"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not planned or has too few players", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> startRequest(
            @Parameter(description = "Request UUID", required = true) @PathVariable("id") UUID requestId,
            @Parameter(description = "Caller UUID. Request Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID userId
    ) {
        logger.atInfo()
                .setMessage("Organizer is starting an activity request")
                .addKeyValue("API endpoint", "POST /api/v1/requests/{id}/start")
                .addKeyValue("requestId", requestId)
                .addKeyValue("userId", userId)
                .log();
        activityRequestService.startRequest(requestId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Geo-search for requests",
            description = "Returns future planned requests with open registration within the search radius, ordered by distance. Sport and date filters are optional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nearby requests, possibly empty"),
            @ApiResponse(responseCode = "400", description = "Invalid coordinates, radius or date range", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<ActivityRequestFeedDto>> searchNearbyRequests(
            @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
            @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
            @Parameter(description = "Latitude", example = "55.7558")
            @RequestParam("lat") Double lat,

            @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
            @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
            @Parameter(description = "Longitude", example = "37.6173")
            @RequestParam("lon") Double lon,
            
            @Parameter(description = "Radius of search in meters", example = "5000")
            @Positive(message = "Radius cannot be a negative number")
            @RequestParam(value = "radius", defaultValue = "5000") Double radius,
            
            @Parameter(description = "Filter by sport name used as its ID (optional)", example = "Футбол")
            @RequestParam(value = "sportId", required = false) String sportId,

            @Parameter(description = "Begin of time window (ISO-8601)", example = "2026-09-10T10:00:00Z") 
            @RequestParam(value = "startDate", required = false) Instant startDate,
            
            @Parameter(description = "End of time window (ISO-8601)", example = "2026-09-12T22:00:00Z") 
            @RequestParam(value = "endDate", required = false) Instant endDate
    ) {
        logger.atInfo()
                .setMessage("User is searching for nearby activity requests")
                .addKeyValue("API endpoint", "GET /api/v1/requests")
                .addKeyValue("lat", lat)
                .addKeyValue("lon", lon)
                .addKeyValue("radius", radius)
                .addKeyValue("sportId", sportId)
                .addKeyValue("startDate", startDate)
                .addKeyValue("endDate", endDate)
                .log();
        
        List<ActivityRequestFeedDto> feed = activityRequestService
                .searchNearbyRequests(lat, lon, radius, sportId, startDate, endDate);
                
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/users/{userId}")
    @Operation(
            summary = "Get a user's activity requests",
            description = "Returns requests organized by the specified user or with their stored participation record, including pending and kicked participants and inactive requests. Players who left or were rejected have no participant record and are not included. " +
                    "Omit statuses to include every request status; set forOrganizer to true to return only requests organized by this user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of requests, possibly empty"),
            @ApiResponse(responseCode = "400", description = "Invalid user UUID, page number or request status",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PagedModel<ActivityRequestInformationDto>> getUserRequestsById(
            @Parameter(description = "UUID of the user whose requests are returned",
                    example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable("userId") UUID userId,

            @Parameter(description = "Optional request statuses. Repeat this query parameter to select multiple statuses; omit it for all statuses.",
                    example = "COMPLETED")
            @RequestParam(value = "statuses", required = false) List<RequestStatus> statuses,

            @Parameter(description = "Zero-based page number; each page contains up to 20 requests.", example = "0")
            @RequestParam(value = "page", defaultValue = "0") Integer pageNumber,

            @Parameter(description = "If true, return only requests organized by the specified user; if false, include requests they joined.",
                    example = "false")
            @RequestParam(value = "forOrganizer", defaultValue = "false") Boolean forOrganizer
    ) {
        Page<ActivityRequestInformationDto> result =
                activityRequestService.getRequestsByUserId(
                        userId,
                        statuses == null ? null : new HashSet<>(statuses),
                        pageNumber,
                        forOrganizer
                );
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request details", description = "Returns the request, current registration state and participant list. Participants may be PENDING, ACCEPTED or KICKED; only ACCEPTED players count towards currentPlayers. After the start, Game owns results and sends the completion status asynchronously.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request details retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed request UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ActivityRequestDetailsDto> getRequestDetails(
            @Parameter(description = "Request UUID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID requestId
    ) {
        logger.atInfo()
                .setMessage("User is fetching activity request details")
                .addKeyValue("API endpoint", "GET /api/v1/requests/{id}")
                .addKeyValue("requestId", requestId)
                .log();
        ActivityRequestDetailsDto details = activityRequestService.getRequestDetails(requestId);
        return ResponseEntity.ok(details);
    }
}
