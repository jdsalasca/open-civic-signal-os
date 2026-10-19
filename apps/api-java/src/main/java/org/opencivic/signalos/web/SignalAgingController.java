package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.SignalAgingService;
import org.opencivic.signalos.web.dto.SignalAgingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signals/aging")
public class SignalAgingController {
    private final SignalAgingService agingService;

    public SignalAgingController(SignalAgingService agingService) {
        this.agingService = agingService;
    }

    @GetMapping
    public SignalAgingResponse getAging(
        @RequestParam(required = false) UUID communityId,
        @RequestParam(required = false) Long slaTargetDays,
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityHeader,
        Principal principal
    ) {
        UUID scope = communityId != null ? communityId : communityHeader;
        return agingService.getAging(scope, principal.getName(), slaTargetDays);
    }
}