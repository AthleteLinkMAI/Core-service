package team.anonyms.athletelink.coreservice.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.anonyms.athletelink.coreservice.dto.CreateActivityRequestDto;
import team.anonyms.athletelink.coreservice.services.ActivityRequestService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/requests")
public class ActivityRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityRequestController.class);

    private final ActivityRequestService activityRequestService;

    public ActivityRequestController(ActivityRequestService activityRequestService) {
        this.activityRequestService = activityRequestService;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> createRequest(
            @RequestBody @Valid CreateActivityRequestDto dto,
            @RequestHeader("X-User-Id") String userId
    ) {
        logger.info("User {} is creating a new activity request: {}", userId, dto.title());

        UUID createdRequestId = activityRequestService.createRequest(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("requestId", createdRequestId));
    }
}