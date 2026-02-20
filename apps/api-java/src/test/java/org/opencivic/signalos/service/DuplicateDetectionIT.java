package org.opencivic.signalos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DuplicateDetectionIT {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private SignalRepository signalRepository;

    @Test
    void shouldDetectNearDuplicateTitlesInSameCategory() {
        signalRepository.deleteAll();

        Signal a = saveSignal("Pothole on Main Street near school", "infrastructure");
        Signal b = saveSignal("Pothole at Main St near the school!", "infrastructure");
        saveSignal("Pothole on Main Street near school", "safety");

        Map<UUID, List<Signal>> duplicates = prioritizationService.findDuplicates();

        assertFalse(duplicates.isEmpty());
        int totalDuplicates = duplicates.values().stream().mapToInt(List::size).sum();
        assertEquals(1, totalDuplicates);

        boolean foundCluster = duplicates.entrySet().stream()
            .anyMatch(entry ->
                entry.getKey().equals(a.getId()) &&
                entry.getValue().stream().anyMatch(s -> s.getId().equals(b.getId()))
            );
        assertTrue(foundCluster);
    }

    private Signal saveSignal(String title, String category) {
        return signalRepository.save(new Signal(
            UUID.randomUUID(),
            title,
            "Reported by integration test",
            category,
            4,
            4,
            45,
            0,
            0.0,
            new ScoreBreakdown(120, 100, 4.5, 0),
            "NEW",
            new ArrayList<>(),
            UUID.randomUUID(),
            LocalDateTime.now().minusMinutes(5)
        ));
    }
}
