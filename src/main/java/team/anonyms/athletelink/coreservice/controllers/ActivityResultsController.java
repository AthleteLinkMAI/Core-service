package team.anonyms.athletelink.coreservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.coreservice.dto.CompletionAcceptedDto;
import team.anonyms.athletelink.coreservice.dto.RecordRoundResultRequestDto;
import team.anonyms.athletelink.coreservice.dto.RoundSubmissionAcceptedDto;
import team.anonyms.athletelink.coreservice.exceptions.GlobalExceptionHandler.ApiError;
import team.anonyms.athletelink.coreservice.services.ActivityResultsService;

import java.util.UUID;

@RestController
@RequestMapping("/requests/{requestId}")
@Tag(name = "Round submissions and completion", description = "Game Service stores and scores round results asynchronously")
@RequiredArgsConstructor
public class ActivityResultsController {
    private final ActivityResultsService activityResultsService;

    @PostMapping("/rounds/{roundNumber}/result")
    @Operation(summary = "Submit one round result", description = "Core validates the organizer and players, then queues the result for Game Service. A 202 response does not mean Game Service has recorded or scored it. Repeated submissions are resolved by Game Service using request ID and round number.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Round result queued"),
            @ApiResponse(responseCode = "400", description = "Invalid round number or player selection", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not active", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RoundSubmissionAcceptedDto> recordRoundResult(
            @Parameter(description = "Request UUID", required = true) @PathVariable UUID requestId,
            @Parameter(description = "Round number, starting at 1", required = true) @PathVariable Integer roundNumber,
            @Parameter(description = "Caller UUID supplied by the trusted API Gateway; Core does not verify the JWT")
            @RequestHeader("X-User-Id") UUID organizerId,
            @Valid @RequestBody RecordRoundResultRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                activityResultsService.recordRoundResult(requestId, roundNumber, organizerId, dto));
    }

    @PostMapping("/complete")
    @Operation(summary = "Request asynchronous completion", description = "Core moves the request to CONFIRMATION and queues a Game Service check. Poll GET /requests/{requestId} for COMPLETED or ACTIVE with completionErrorMessage. A repeated call while pending returns the same attempt ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Completion check queued"),
            @ApiResponse(responseCode = "403", description = "Caller is not the organizer", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Request is not active", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CompletionAcceptedDto> completeRequest(
            @Parameter(description = "Request UUID", required = true) @PathVariable UUID requestId,
            @Parameter(description = "Caller UUID supplied by the trusted API Gateway; Core does not verify the JWT")
            @RequestHeader("X-User-Id") UUID organizerId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                activityResultsService.completeRequest(requestId, organizerId));
    }
}
