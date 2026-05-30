package com.ghostcoach.controller;

import com.ghostcoach.model.SportType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Exposes the list of supported sports so the frontend drives its sport selector
 * from the API rather than being hardcoded — adding a new SportType value
 * is enough to make it appear in the UI without a frontend deploy.
 */
@RestController
@RequestMapping("/api/sports")
public class SportsController {

    @GetMapping
    public ResponseEntity<List<String>> getSports() {
        List<String> sports = Arrays.stream(SportType.values())
                .map(SportType::name)
                .toList();
        return ResponseEntity.ok(sports);
    }
}
