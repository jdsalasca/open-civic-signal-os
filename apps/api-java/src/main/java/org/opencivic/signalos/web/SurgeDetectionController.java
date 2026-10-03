package org.opencivic.signalos.web;

import java.security.Principal;
import java.time.LocalDate;
import org.opencivic.signalos.service.SurgeDetectionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signals/surges")
public class SurgeDetectionController {
    private final SurgeDetectionService surgeService;

    public SurgeDetectionController(SurgeDetectionService surgeService) {
        this.surgeService = surgeService;
    }

    /**
     * Thresholds are query parameters rather than configuration so a community can tune what
     * counts as a surge for its own volume without a deployment. They are echoed back in the
     * response, so a surge is never reported without the numbers that produced it.
     */
    @GetMapping
    public SurgeDetectionService.SurgeReport getSurges(
        @RequestParam(defaultValue = "false") boolean allowGlobal,
        @RequestParam(required = false) Integer windowDays,
        @RequestParam(required = false) Integer baselineWindows,
        @RequestParam(required = false) Integer minCurrentCount,
        @RequestParam(required = false) Double surgeMultiplier,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate windowEnd,
        Principal principal
    ) {
        return surgeService.buildReport(
            principal.getName(),
            allowGlobal,
            windowDays,
            baselineWindows,
            minCurrentCount,
            surgeMultiplier,
            windowEnd
        );
    }

    /** The formula on its own, so a dashboard can show "why" without fetching the whole report. */
    @GetMapping("/formula")
    public SurgeDetectionService.SurgeReport formula(
        @RequestParam(defaultValue = "false") boolean allowGlobal,
        Principal principal
    ) {
        return surgeService.buildReport(
            principal.getName(), allowGlobal, null, null, null, null, null);
    }
}