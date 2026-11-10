package team.anonyms.athletelink.requestservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.anonyms.athletelink.requestservice.dto.JoinDecisionRequestDto;
import team.anonyms.athletelink.requestservice.dto.PendingJoinRequestDto;
import team.anonyms.athletelink.requestservice.exceptions.GlobalExceptionHandler.ApiError;
import team.anonyms.athletelink.requestservice.services.ActivityRequestService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/requests/{requestId}/join-requests")
@Tag(name = "Join requests", description = "Organizer review of pending player applications before an activity request starts")
public class JoinRequestsController {
    private final ActivityRequestService activityRequestService;

    public JoinRequestsController(ActivityRequestService activityRequestService) {
        this.activityRequestService = activityRequestService;
    }

    @GetMapping
    @Operation(summary = "List pending join requests",
            description = "Returns pending applications for this request. Only its organizer may view them. "
                    + "A participantId identifies one application and is required when deciding it. "
                    + "An empty array means there are no pending applications. Registration may be closed; existing applications remain visible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending applications, possibly empty",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PendingJoinRequestDto.class)))),
            @ApiResponse(responseCode = "400", description = "Missing or malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Activity request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Activity request is no longer planned", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<PendingJoinRequestDto>> getPendingJoinRequests(
            @Parameter(description = "Activity request UUID", required = true) @PathVariable("requestId") UUID requestId,
            @Parameter(description = "Caller UUID supplied by the API Gateway. Request Service does not authenticate this header itself.", required = true)
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        return ResponseEntity.ok(activityRequestService.getPendingJoinRequests(requestId, organizerId));
    }

    @PostMapping("/{participantId}/decision")
    @Operation(summary = "Approve or reject a join request",
            description = "Only the organizer may decide a pending application while the activity request is PLANNED. "
                    + "Approval changes the player to ACCEPTED immediately and sets joinedAt. "
                    + "Rejection deletes the pending record. A later application by the same user has a different participantId. "
                    + "The decision is committed in Request Service; no Game Service message is used. "
                    + "Closing registration blocks new applications but does not block decisions on existing ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Decision applied; no response body"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed UUID or accepted flag", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Activity request or specified application not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Activity request is no longer planned or the application is no longer pending", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> decideJoinRequest(
            @Parameter(description = "Activity request UUID", required = true) @PathVariable("requestId") UUID requestId,
            @Parameter(description = "ID of the pending participant record returned by the list endpoint", required = true)
            @PathVariable("participantId") UUID participantId,
            @Parameter(description = "Caller UUID supplied by the API Gateway. Request Service does not authenticate this header itself.", required = true)
            @RequestHeader("X-User-Id") UUID organizerId,
            @Valid @RequestBody JoinDecisionRequestDto decision
    ) {
        activityRequestService.decideJoinRequest(requestId, participantId, organizerId, decision.accepted());
        return ResponseEntity.noContent().build();
    }
}
