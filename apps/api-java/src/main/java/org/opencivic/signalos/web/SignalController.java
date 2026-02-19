package org.opencivic.signalos.web;

import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.service.PrioritizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
