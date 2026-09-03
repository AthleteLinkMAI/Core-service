package team.anonyms.athletelink.coreservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Sports list", description = "API for receiving the list of available sports. Used for rendering dropdowns on the frontend.")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    @Operation(
            summary = "Get all sports", 
            description = "Returns a list of all available sports from the database. Used for rendering dropdowns on the frontend."
    )
    public ResponseEntity<List<SportDto>> getAllSports() {
        return ResponseEntity.ok(sportService.getAllSports());
    }
}