package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.HelpCenterService;
import org.opencivic.signalos.web.dto.HelpCenterResponse;
import org.opencivic.signalos.web.dto.HelpCenterStateResponse;
import org.opencivic.signalos.web.dto.UpdateHelpCenterStateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/help-center")
public class HelpCenterController {
    private final HelpCenterService helpCenterService;

    public HelpCenterController(HelpCenterService helpCenterService) {
        this.helpCenterService = helpCenterService;
    }

    @GetMapping
    public HelpCenterResponse getHelpCenter(
        Principal principal,
        @RequestHeader(name = "X-Community-Id", required = false) UUID communityId,
        @RequestParam(name = "lang", required = false) String language,
        @RequestParam(name = "surface", required = false) String surface,
        @RequestParam(name = "query", required = false) String query
    ) {
        return helpCenterService.getHelpCenter(principal.getName(), communityId, language, surface, query);
    }

    @PutMapping("/state")
    public HelpCenterStateResponse updateState(
        Principal principal,
        @Valid @RequestBody UpdateHelpCenterStateRequest request
    ) {
        return helpCenterService.updateState(principal.getName(), request);
    }
}
