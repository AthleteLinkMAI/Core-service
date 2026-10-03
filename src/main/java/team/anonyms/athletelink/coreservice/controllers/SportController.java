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
@Tag(name = "Sports", description = "Read the sport catalog and whether each sport accepts new requests")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    @Operation(
            summary = "List sports",
            description = "Returns all sports, including those no longer playable, with player limits and isPlayable."
    )
    @ApiResponse(responseCode = "200", description = "Sport catalog, possibly empty")
    public ResponseEntity<List<SportDto>> getAllSports() {
        return ResponseEntity.ok(sportService.getAllSports());
    }
}
