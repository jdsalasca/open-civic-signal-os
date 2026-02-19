package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.PrioritizationService;
import org.opencivic.signalos.service.ExportService;
import org.opencivic.signalos.web.dto.SignalCreateRequest;
import org.opencivic.signalos.web.dto.SignalResponse;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    private final PrioritizationService prioritizationService;
    private final ExportService exportService;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;

    public SignalController(PrioritizationService prioritizationService, ExportService exportService, UserRepository userRepository, SignalRepository signalRepository) {
        this.prioritizationService = prioritizationService;
        this.exportService = exportService;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
    }

    @GetMapping("/prioritized")
    public Page<SignalResponse> getPrioritizedSignals(
            @PageableDefault(size = 20, sort = "priorityScore", direction = Sort.Direction.DESC) Pageable pageable) {
        return prioritizationService.getPrioritizedSignals(pageable)
                .map(this::mapToResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignalResponse> getSignalById(@PathVariable UUID id) {
        return prioritizationService.getSignalById(id)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Civic signal not found: " + id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SignalResponse> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String newStatusStr = body.get("status");
        if (newStatusStr == null || newStatusStr.isBlank()) {
            throw new IllegalArgumentException("Status field is mandatory.");
        }
        return prioritizationService.updateStatus(id, newStatusStr)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Signal not found for status update: " + id));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<SignalResponse> vote(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(mapToResponse(prioritizationService.voteForSignal(id, authentication.getName())));
    }

    @GetMapping("/flagged")
    public Page<SignalResponse> getFlaggedSignals(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return prioritizationService.getFlaggedSignals(pageable)
                .map(this::mapToResponse);
    }

    @PostMapping("/{id}/moderate")
    public SignalResponse moderate(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return mapToResponse(prioritizationService.moderateSignal(id, body.get("action"), body.get("reason")));
    }

    // OCS-P2-011: Data Export restricted to SUPER_ADMIN
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
    public List<SignalResponse> getTopUnresolved() {
        return prioritizationService.getTopUnresolved(10).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/mine")
    public List<SignalResponse> getMySignals(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return signalRepository.findByAuthorIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public SignalResponse createSignal(@Valid @RequestBody SignalCreateRequest request, Authentication authentication) {
        User author = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Signal s = new Signal(
            UUID.randomUUID(), request.title(), request.description(), request.category(),
            request.urgency(), request.impact(), request.affectedPeople(),
            0, 0.0, null, SignalStatus.NEW.name(), new ArrayList<>(), author.getId(), LocalDateTime.now()
        );
        s.setPriorityScore(prioritizationService.calculateScore(s));
        s.setScoreBreakdown(prioritizationService.getBreakdown(s));
        return mapToResponse(prioritizationService.saveSignal(s));
    }

    @GetMapping("/duplicates")
    public Map<UUID, List<SignalResponse>> getDuplicates() {
        return prioritizationService.findDuplicates().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream().map(this::mapToResponse).collect(Collectors.toList())
                ));
    }

    @PostMapping("/merge")
    public ResponseEntity<SignalResponse> mergeSignals(@RequestParam UUID targetId, @RequestBody List<UUID> duplicateIds) {
        Signal merged = prioritizationService.mergeSignals(targetId, duplicateIds);
        return ResponseEntity.ok(mapToResponse(merged));
    }

    private SignalResponse mapToResponse(Signal s) {
        return new SignalResponse(
            s.getId(), s.getTitle(), s.getDescription(), s.getCategory(),
            s.getStatus(), s.getPriorityScore(), s.getScoreBreakdown()
        );
    }
}
