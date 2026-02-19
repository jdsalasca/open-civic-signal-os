package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.service.PrioritizationService;
import org.opencivic.signalos.web.dto.SignalCreateRequest;
import org.opencivic.signalos.web.dto.SignalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

    public SignalController(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;
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
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SignalResponse> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return prioritizationService.updateStatus(id, newStatus)
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/top-10")
    public List<SignalResponse> getTopUnresolved() {
        return prioritizationService.getTopUnresolved(10).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public SignalResponse createSignal(@Valid @RequestBody SignalCreateRequest request) {
        Signal s = new Signal(
            UUID.randomUUID(),
            request.title(),
            request.description(),
            request.category(),
            request.urgency(),
            request.impact(),
            request.affectedPeople(),
            0, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now()
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
    public SignalResponse mergeSignals(@RequestParam UUID targetId, @RequestBody List<UUID> duplicateIds) {
        Signal merged = prioritizationService.mergeSignals(targetId, duplicateIds);
        return merged != null ? mapToResponse(merged) : null;
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
}
