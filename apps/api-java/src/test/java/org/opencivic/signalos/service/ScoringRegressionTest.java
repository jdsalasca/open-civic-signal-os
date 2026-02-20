package org.opencivic.signalos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ScoringRegressionTest {

    @Autowired
    private PrioritizationService prioritizationService;

    @ParameterizedTest(name = "Urgency={0}, Impact={1}, People={2}, Votes={3} => Score={4}")
    @CsvSource({
        "1, 1, 1, 0, 55.1",      // Min urgency(30) + Min impact(25) + Min people(0.1) + Min votes(0) -> 55.1
        "5, 5, 300, 75, 320.0",  // Max urgency(150) + Max impact(125) + Max people(30) + Max votes(15) -> 320
        "3, 3, 100, 10, 177.0",  // Mid(90) + Mid(75) + 10 + 2 -> 177
        "5, 1, 1, 0, 175.1"      // High urgency, low impact -> 150 + 25 + 0.1 -> 175.1
    })
    @DisplayName("Regression: Core prioritization formula must remain deterministic")
    void verifyScoringFormula(int urgency, int impact, int affectedPeople, int votes, double expectedScore) {
        Signal signal = new Signal(
            UUID.randomUUID(), 
            "Regression Test", 
            "Checking formula stability", 
            "safety",
            urgency, 
            impact, 
            affectedPeople, 
            votes, 
            0.0, 
            null, 
            SignalStatus.NEW.name(), 
            new ArrayList<>(), 
            UUID.randomUUID(), 
            LocalDateTime.now()
        );

        ScoreBreakdown breakdown = prioritizationService.getBreakdown(signal);
        double actualScore = breakdown.urgency() + breakdown.impact() + breakdown.affectedPeople() + breakdown.communityVotes();

        assertEquals(expectedScore, actualScore, 0.01, "Scoring formula deviation detected!");
    }
}
