package org.opencivic.signalos.web;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.service.PrioritizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    private final PrioritizationService prioritizationService;

    public SignalController(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;
    }

    @GetMapping("/prioritized")
    public List<Signal> getPrioritizedSignals() {
        return prioritizationService.getPrioritizedSignals();
    }

    @GetMapping("/top-10")
    public List<Signal> getTopUnresolved() {
        return prioritizationService.getTopUnresolved(10);
    }

    @GetMapping("/duplicates")
    public Map<UUID, List<Signal>> getDuplicates() {
        return prioritizationService.findDuplicates();
    }

    @PostMapping("/merge")
    public Signal mergeSignals(@RequestParam UUID targetId, @RequestBody List<UUID> duplicateIds) {
        return prioritizationService.mergeSignals(targetId, duplicateIds);
    }
}
