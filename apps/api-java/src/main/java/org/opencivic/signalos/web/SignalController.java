package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.CommunityAccessService;
import org.opencivic.signalos.service.ExportService;
import org.opencivic.signalos.service.PrioritizationService;
import org.opencivic.signalos.web.dto.SignalCreateRequest;
import org.opencivic.signalos.web.dto.SignalMetaResponse;
import org.opencivic.signalos.web.dto.SignalResponse;
import org.opencivic.signalos.service.CivicEngagementService;
import org.opencivic.signalos.web.dto.CivicCommentResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.opencivic.signalos.web.dto.TrustPacket;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Collection<String> RESOLVED_STATUSES = List.of("RESOLVED", "REJECTED");
    private final PrioritizationService prioritizationService;
    private final ExportService exportService;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final CommunityAccessService communityAccessService;
    private final MeterRegistry meterRegistry;
    private final CivicEngagementService engagementService;

    public SignalController(
        PrioritizationService prioritizationService,
        ExportService exportService,
        UserRepository userRepository,
        SignalRepository signalRepository,
        CommunityAccessService communityAccessService,
        MeterRegistry meterRegistry,
        CivicEngagementService engagementService
    ) {
        this.prioritizationService = prioritizationService;
        this.exportService = exportService;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.communityAccessService = communityAccessService;
        this.meterRegistry = meterRegistry;
        this.engagementService = engagementService;
    }

    @GetMapping("/{id}/comments")
    public List<CivicCommentResponse> getComments(@PathVariable UUID id) {
        return engagementService.getComments(id, "SIGNAL");
    }

    @PostMapping("/{id}/comments")
    public CivicCommentResponse addComment(@PathVariable UUID id, @RequestBody Map<String, String> body, Authentication auth) {
        return engagementService.addComment(id, "SIGNAL", body.get("content"), auth.getName());
    }

    @PostMapping("/{id}/react")
    public Map<String, Integer> react(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return engagementService.react(id, "SIGNAL", body.get("type"));
    }

    @GetMapping("/{id}/trust-packet")
    public ResponseEntity<TrustPacket> getTrustPacket(@PathVariable UUID id) {
        return ResponseEntity.ok(prioritizationService.getTrustPacket(id));
    }

    @GetMapping("/{id}/history")
    public List<org.opencivic.signalos.domain.SignalStatusEntry> getStatusHistory(@PathVariable UUID id) {
        return prioritizationService.getStatusHistory(id);
    }

    @GetMapping("/prioritized")
    public Page<SignalResponse> getPrioritizedSignals(
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        @RequestParam(value = "status", required = false) String statusFilter,
        Authentication authentication,
        @PageableDefault(size = 20, sort = "priorityScore", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String scope = communityId == null ? "global" : "community";
        Timer.Sample latencySample = Timer.start(meterRegistry);
        String status = "success";
        validateCommunityScope(authentication, communityId);
        List<String> statuses = normalizeStatusFilter(statusFilter);
        Pageable sanitized = PageRequest.of(
            pageable.getPageNumber(),
            Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
            pageable.getSort()
        );
        try {
            Page<SignalResponse> response = prioritizationService.getPrioritizedSignals(sanitized, communityId, statuses)
                .map(this::mapToResponse);

            meterRegistry.counter("signalos.prioritized.requests.total", "scope", scope).increment();
            DistributionSummary.builder("signalos.prioritized.result.size")
                .baseUnit("signals")
                .tag("scope", scope)
                .register(meterRegistry)
                .record(response.getNumberOfElements());

            return response;
        } catch (RuntimeException ex) {
            status = "error";
            meterRegistry.counter("signalos.prioritized.requests.errors.total", "scope", scope).increment();
            throw ex;
        } finally {
            latencySample.stop(
                Timer.builder("signalos.prioritized.latency")
                    .description("Latency for prioritized feed retrieval")
                    .tag("scope", scope)
                    .tag("status", status)
                    .register(meterRegistry)
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignalResponse> getSignalById(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        return prioritizationService.getSignalById(id, communityId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Civic signal not found: " + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SignalResponse> updateStatus(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        @RequestBody Map<String, String> body,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        String newStatusStr = body.get("status");
        if (newStatusStr == null || newStatusStr.isBlank()) {
            throw new IllegalArgumentException("Status field is mandatory.");
        }
        return prioritizationService.updateStatus(id, newStatusStr, communityId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResourceNotFoundException("Signal not found for status update: " + id));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<SignalResponse> vote(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        return ResponseEntity.ok(
            mapToResponse(prioritizationService.voteForSignal(id, authentication.getName(), communityId))
        );
    }

    @GetMapping("/flagged")
    public Page<SignalResponse> getFlaggedSignals(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable sanitized = PageRequest.of(
            pageable.getPageNumber(),
            Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
            pageable.getSort()
        );
        return prioritizationService.getFlaggedSignals(sanitized)
            .map(this::mapToResponse);
    }

    @PostMapping("/{id}/moderate")
    public SignalResponse moderate(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return mapToResponse(prioritizationService.moderateSignal(id, body.get("action"), body.get("reason")));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Resource> exportCsv() {
        String filename = "signalos_intelligence_export_" + LocalDateTime.now() + ".csv";
        InputStreamResource file = new InputStreamResource(exportService.exportSignalsToCsv());

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(file);
    }

    @GetMapping("/top-10")
    public List<SignalResponse> getTopUnresolved(
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        return prioritizationService.getTopUnresolved(10, communityId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @GetMapping("/meta")
    public SignalMetaResponse getSignalsMeta(
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        long totalSignals = communityId == null
            ? signalRepository.count()
            : signalRepository.countByCommunityId(communityId);

        long unresolvedSignals = communityId == null
            ? signalRepository.countByStatusNotIn(RESOLVED_STATUSES)
            : signalRepository.countByStatusNotInAndCommunityId(RESOLVED_STATUSES, communityId);

        LocalDateTime lastUpdatedAt = (communityId == null
            ? signalRepository.findTopByOrderByCreatedAtDesc()
            : signalRepository.findTopByCommunityIdOrderByCreatedAtDesc(communityId))
                .map(Signal::getCreatedAt)
                .orElse(null);

        return new SignalMetaResponse(totalSignals, unresolvedSignals, lastUpdatedAt);
    }

    @GetMapping("/mine")
    public Page<SignalResponse> getMySignals(
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        Authentication authentication
    ) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        validateCommunityScope(authentication, communityId);
        Pageable sanitized = PageRequest.of(
            pageable.getPageNumber(),
            Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
            pageable.getSort()
        );
        return (communityId == null
            ? signalRepository.findByAuthorId(user.getId(), sanitized)
            : signalRepository.findByAuthorIdAndCommunityId(user.getId(), communityId, sanitized))
            .map(this::mapToResponse);
    }

    @PostMapping
    public SignalResponse createSignal(
        @Valid @RequestBody SignalCreateRequest request,
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId,
        Authentication authentication
    ) {
        validateCommunityScope(authentication, communityId);
        Signal s = prioritizationService.createSignal(
            request.title(),
            request.description(),
            request.category(),
            request.urgency(),
            request.impact(),
            request.affectedPeople(),
            authentication.getName(),
            communityId
        );
        return mapToResponse(s);
    }

    @GetMapping("/duplicates")
    public Map<UUID, List<SignalResponse>> getDuplicates(
        @RequestHeader(value = "X-Community-Id", required = false) UUID communityId
    ) {
        return prioritizationService.findDuplicates(communityId).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream().map(this::mapToResponse).collect(Collectors.toList())
            ));
    }

    @PostMapping("/merge")
    public ResponseEntity<SignalResponse> mergeSignals(
        @RequestParam UUID targetId,
        @RequestBody List<UUID> duplicateIds
    ) {
        Signal merged = prioritizationService.mergeSignals(targetId, duplicateIds);
        return ResponseEntity.ok(mapToResponse(merged));
    }

    private SignalResponse mapToResponse(Signal s) {
        return new SignalResponse(
            s.getId(),
            s.getTitle(),
            s.getDescription(),
            s.getCategory(),
            s.getStatus(),
            s.getPriorityScore(),
            s.getScoreBreakdown()
        );
    }

    private void validateCommunityScope(Authentication authentication, UUID communityId) {
        if (communityId == null || authentication == null) {
            return;
        }
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        communityAccessService.requireMembership(user.getId(), communityId);
    }

    private List<String> normalizeStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return List.of();
        }
        return Arrays.stream(statusFilter.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toUpperCase)
            .distinct()
            .toList();
    }
}
