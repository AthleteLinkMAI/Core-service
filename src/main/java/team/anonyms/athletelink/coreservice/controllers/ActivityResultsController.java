package team.anonyms.athletelink.coreservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.RoundResultResponseDto;
import team.anonyms.athletelink.coreservice.exceptions.GlobalExceptionHandler.ApiError;
import team.anonyms.athletelink.coreservice.services.ActivityResultsService;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/requests/{requestId}")
@Tag(name = "Round results", description = "Organizer-submitted round outcomes and request completion")
public class ActivityResultsController {

    private final ActivityResultsService activityResultsService;

    public ActivityResultsController(ActivityResultsService activityResultsService) {
        this.activityResultsService = activityResultsService;
    }

    @PostMapping("/rounds/{roundNumber}/result")
    @Operation(
            summary = "Record a round result",
            description = "The organizer records equal-sized, non-empty winner and loser groups from accepted participants. A round result is immutable and can be recorded only while the request is active. Rating calculation is not performed by Core Service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Round result recorded"),
            @ApiResponse(responseCode = "400", description = "Invalid round number or player selection", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not active or this round already has a result", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RoundResultResponseDto> recordRoundResult(
            @Parameter(description = "Request UUID", required = true) @PathVariable UUID requestId,
            @Parameter(description = "Round number, starting at 1", example = "1", required = true) @PathVariable Integer roundNumber,
            @Parameter(description = "Caller UUID. Core Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID organizerId,
            @Valid @RequestBody RecordRoundResultRequestDto dto
    ) {
        RoundResultResponseDto result =
                activityResultsService.recordRoundResult(
                        requestId, roundNumber, organizerId, dto
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/rounds")
    @Operation(summary = "List recorded round results", description = "Returns all recorded round results for the request, ordered by round number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recorded results, possibly empty"),
            @ApiResponse(responseCode = "400", description = "Malformed request UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<RoundResultResponseDto> getRoundResults(
            @Parameter(description = "Request UUID", required = true) @PathVariable UUID requestId
    ) {
        return activityResultsService.getRoundResults(requestId);
    }

    @PostMapping("/complete")
    @Operation(summary = "Complete a request", description = "The organizer completes an active request after results have been recorded for every planned round.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Request completed"),
            @ApiResponse(responseCode = "400", description = "Malformed request or caller UUID", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not active or some rounds have no result", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> completeRequest(
            @Parameter(description = "Request UUID", required = true) @PathVariable UUID requestId,
            @Parameter(description = "Caller UUID. Core Service does not currently authenticate this header.", required = true)
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        activityResultsService.completeRequest(requestId, organizerId);
        return ResponseEntity.noContent().build();
    }
}
