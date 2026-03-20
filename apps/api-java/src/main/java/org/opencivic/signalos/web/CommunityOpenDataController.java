package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityOpenDataExportType;
import org.opencivic.signalos.domain.CommunityOpenDataFormat;
import org.opencivic.signalos.service.CommunityOpenDataService;
import org.opencivic.signalos.web.dto.CommunityOpenDataCenterResponse;
import org.opencivic.signalos.web.dto.CommunityOpenDataTokenResponse;
import org.opencivic.signalos.web.dto.CreateCommunityOpenDataTokenRequest;
import org.opencivic.signalos.web.dto.CreateCommunityOpenDataTokenResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/exports")
public class CommunityOpenDataController {
    private final CommunityOpenDataService openDataService;

    public CommunityOpenDataController(CommunityOpenDataService openDataService) {
        this.openDataService = openDataService;
    }

    @GetMapping("/center")
    public CommunityOpenDataCenterResponse getCenter(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return openDataService.getCenter(communityId, principal.getName());
    }

    @PostMapping("/tokens")
    public CreateCommunityOpenDataTokenResponse createToken(
        @Valid @RequestBody CreateCommunityOpenDataTokenRequest request,
        Principal principal
    ) {
        return openDataService.createToken(request, principal.getName());
    }

    @DeleteMapping("/tokens/{tokenId}")
    public CommunityOpenDataTokenResponse revokeToken(
        @PathVariable UUID tokenId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return openDataService.revokeToken(communityId, tokenId, principal.getName());
    }

    @GetMapping("/{exportType}")
    public ResponseEntity<Resource> exportDataset(
        @PathVariable String exportType,
        @RequestParam UUID communityId,
        @RequestParam(defaultValue = "CSV") String format,
        Principal principal
    ) {
        CommunityOpenDataService.ExportPayload payload = openDataService.exportForUser(
            communityId,
            CommunityOpenDataExportType.valueOf(exportType.trim().toUpperCase()),
            CommunityOpenDataFormat.valueOf(format.trim().toUpperCase()),
            principal.getName()
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + payload.filename())
            .header(HttpHeaders.CONTENT_TYPE, payload.contentType())
            .body(new ByteArrayResource(payload.body()));
    }
}
