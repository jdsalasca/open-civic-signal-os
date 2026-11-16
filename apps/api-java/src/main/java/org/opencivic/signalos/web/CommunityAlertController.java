package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityAlertService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communities/{communityId}/alerts")
public class CommunityAlertController {
    private final CommunityAlertService alertService;

    public CommunityAlertController(CommunityAlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public Map<String, Boolean> status(@PathVariable UUID communityId, Principal principal) {
        return Map.of("subscribed", alertService.isSubscribed(communityId, principal.getName()));
    }

    @PostMapping
    public Map<String, Boolean> subscribe(@PathVariable UUID communityId, Principal principal) {
        alertService.subscribe(communityId, principal.getName());
        return Map.of("subscribed", true);
    }

    @DeleteMapping
    public Map<String, Boolean> unsubscribe(@PathVariable UUID communityId, Principal principal) {
        alertService.unsubscribe(communityId, principal.getName());
        return Map.of("subscribed", false);
    }
}
