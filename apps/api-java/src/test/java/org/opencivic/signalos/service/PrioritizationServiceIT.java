package org.opencivic.signalos.service;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class PrioritizationServiceIT {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private SignalRepository signalRepository;

    @Test
    public void shouldCalculateScoreAndPersistSignal() {
        // Given
        Signal signal = new Signal(
            UUID.randomUUID(),
            "Test Signal",
            "Description",
            "infrastructure",
            5, // urgency
            5, // impact
            100, // affected
            0, 0.0, null, "NEW", new ArrayList<>(), LocalDateTime.now()
        );

        // When
        double calculatedScore = prioritizationService.calculateScore(signal);
        signal.setPriorityScore(calculatedScore);
        signal.setScoreBreakdown(prioritizationService.getBreakdown(signal));
        
        Signal saved = prioritizationService.saveSignal(signal);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPriorityScore()).isEqualTo(calculatedScore);
        assertThat(saved.getScoreBreakdown().urgency()).isEqualTo(150.0); // 5 * 30
        
        Page<Signal> page = prioritizationService.getPrioritizedSignals(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
    }
}
