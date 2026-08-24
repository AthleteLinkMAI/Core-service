package team.anonyms.athletelink.coreservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.anonyms.athletelink.coreservice.dto.SportDto;
import team.anonyms.athletelink.coreservice.services.SportService;

import java.util.List;

@RestController
@RequestMapping("/sports")
public class SportController {

    private final SportService sportService;

    public SportController(SportService sportService) {
        this.sportService = sportService;
    }

    @GetMapping
    public ResponseEntity<List<SportDto>> getAllSports() {
        return ResponseEntity.ok(sportService.getAllSports());
    }
}