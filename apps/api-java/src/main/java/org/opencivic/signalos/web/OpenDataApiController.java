package org.opencivic.signalos.web;

import java.util.UUID;
import org.opencivic.signalos.domain.CommunityOpenDataExportType;
import org.opencivic.signalos.service.CommunityOpenDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/open-data")
public class OpenDataApiController {
    private final CommunityOpenDataService openDataService;

    public OpenDataApiController(CommunityOpenDataService openDataService) {
        this.openDataService = openDataService;
    }

    @GetMapping("/{communityId}/{exportType}")
    public ResponseEntity<Object> getScopedExport(
        @PathVariable UUID communityId,
        @PathVariable String exportType,
        @RequestHeader("X-Api-Token") String apiToken
    ) {
        CommunityOpenDataService.TokenApiResult result = openDataService.readWithToken(
            communityId,
            CommunityOpenDataExportType.valueOf(exportType.trim().toUpperCase()),
            apiToken
        );
        return ResponseEntity.ok()
            .header("X-RateLimit-Limit", String.valueOf(result.rateLimitLimit()))
            .header("X-RateLimit-Remaining", String.valueOf(result.rateLimitRemaining()))
            .header("X-RateLimit-Reset", result.rateLimitResetAt().toString())
            .body(result.payload());
    }
}
