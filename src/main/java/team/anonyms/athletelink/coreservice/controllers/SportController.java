package team.anonyms.athletelink.coreservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.coreservice.dto.SportDto;
import team.anonyms.athletelink.coreservice.services.SportService;

import java.util.List;

@RestController
@RequestMapping("/sports")
@Tag(name = "Sports", description = "Read the sports and player limits available for activity requests")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    @Operation(
            summary = "List available sports",
            description = "Returns sports with their minimum and maximum player counts."
    )
    @ApiResponse(responseCode = "200", description = "Available sports, possibly empty")
    public ResponseEntity<List<SportDto>> getAllSports() {
        return ResponseEntity.ok(sportService.getAllSports());
    }
}
