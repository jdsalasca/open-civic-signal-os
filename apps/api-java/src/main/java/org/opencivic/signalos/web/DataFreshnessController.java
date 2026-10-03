package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.service.DataFreshnessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/freshness")
public class DataFreshnessController {

    private final DataFreshnessService freshnessService;

    public DataFreshnessController(DataFreshnessService freshnessService) {
        this.freshnessService = freshnessService;
    }

    @GetMapping
    public List<DataFreshnessService.FreshnessReport> getFreshness(
        @RequestParam(defaultValue = "false") boolean allowGlobal,
        @RequestParam(required = false) UUID communityId,
        Principal principal
    ) {
        return freshnessService.getReports(principal.getName(), allowGlobal, communityId);
    }
}