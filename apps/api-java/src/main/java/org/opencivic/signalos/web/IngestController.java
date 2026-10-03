package org.opencivic.signalos.web;

import java.security.Principal;
import org.opencivic.signalos.service.IngestService;
import org.opencivic.signalos.web.dto.IngestReportResponse;
import org.opencivic.signalos.web.dto.IngestRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {
    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    /**
     * Same contract either way: {@code commit=false} validates and reports without
     * persisting, {@code commit=true} persists the accepted rows. Callers should dry-run
     * first so a data steward can read the row-level errors before writing anything.
     */
    @PostMapping("/validate")
    public IngestReportResponse ingest(
        @RequestBody IngestRequest request,
        Principal principal
    ) {
        return ingestService.ingest(
            new IngestRequest(
                request.communityId(),
                request.source(),
                request.fileName(),
                request.content(),
                false
            ),
            principal.getName()
        );
    }

    @PostMapping("/commit")
    public IngestReportResponse commit(
        @RequestBody IngestRequest request,
        Principal principal
    ) {
        return ingestService.ingest(
            new IngestRequest(
                request.communityId(),
                request.source(),
                request.fileName(),
                request.content(),
                true
            ),
            principal.getName()
        );
    }
}