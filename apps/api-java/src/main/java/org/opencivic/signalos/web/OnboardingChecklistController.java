package org.opencivic.signalos.web;

import java.security.Principal;
import org.opencivic.signalos.service.OnboardingChecklistService;
import org.opencivic.signalos.web.dto.OnboardingChecklistResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingChecklistController {
    private final OnboardingChecklistService checklistService;

    public OnboardingChecklistController(OnboardingChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/checklist")
    public OnboardingChecklistResponse checklist(Principal principal) {
        return checklistService.checklist(principal.getName());
    }
}
