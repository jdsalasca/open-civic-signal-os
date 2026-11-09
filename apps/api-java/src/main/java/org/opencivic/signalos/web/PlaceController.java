package org.opencivic.signalos.web;

import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.service.GeocodingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** External place lookup for the onboarding step; no local dataset is stored. */
@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final GeocodingService geocodingService;

    public PlaceController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/reverse")
    public GeocodingService.PlaceSuggestion reverse(
        @RequestParam double latitude,
        @RequestParam double longitude
    ) {
        return geocodingService.reverse(latitude, longitude)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No place found for these coordinates. Choose a city manually."
            ));
    }
}
