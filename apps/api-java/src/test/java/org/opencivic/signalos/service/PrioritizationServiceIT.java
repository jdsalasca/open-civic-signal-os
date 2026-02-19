package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class PrioritizationServiceIT {

    @Autowired
    private PrioritizationService prioritizationService;

    @Test
    void shouldCalculateCorrectScore() {
        Signal signal = new Signal(UUID.randomUUID(), "Test", "Desc", "safety", 5, 5, 100, 10, 0.0, null, "NEW", new ArrayList<>(), UUID.randomUUID(), LocalDateTime.now());
        
        double score = prioritizationService.calculateScore(signal);
        ScoreBreakdown breakdown = prioritizationService.getBreakdown(signal);

        assertEquals(287.0, score);
        assertEquals(150.0, breakdown.urgency());
        assertEquals(125.0, breakdown.impact());
    }

    @Test
    void shouldAutoFlagSuspiciousSignal() {
        Signal signal = new Signal(UUID.randomUUID(), "Suspicious", "Desc", "infrastructure", 5, 1, 1, 0, 0.0, null, "NEW", new ArrayList<>(), UUID.randomUUID(), LocalDateTime.now());
        
        Signal saved = prioritizationService.saveSignal(signal);
        
        assertEquals("FLAGGED", saved.getStatus());
        assertTrue(saved.getModerationReason().contains("Suspicious"));
    }
}
