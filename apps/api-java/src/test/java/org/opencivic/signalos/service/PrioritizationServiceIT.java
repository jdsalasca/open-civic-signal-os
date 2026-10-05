package org.opencivic.signalos.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PrioritizationServiceIT {

    @Autowired
    private PrioritizationService prioritizationService;

    @Autowired
    private UserRepository userRepository;

    /** Signals carry a foreign key to their author, so a random id is not a signal - it is an orphan. */
    private UUID authorId;

    @BeforeEach
    void createAuthor() {
        User author = new User("prio_author", "{noop}pw", "prio-author@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        authorId = userRepository.save(author).getId();
    }

    @Test
    void shouldCalculateCorrectScore() {
        Signal signal = new Signal(UUID.randomUUID(), "Test", "Desc", "safety", 5, 5, 100, 10, 0.0, null, "NEW", new ArrayList<>(), authorId, LocalDateTime.now());
        
        double score = prioritizationService.calculateScore(signal);
        ScoreBreakdown breakdown = prioritizationService.getBreakdown(signal);

        assertEquals(287.0, score);
        assertEquals(150.0, breakdown.urgency());
        assertEquals(125.0, breakdown.impact());
    }

    @Test
    void shouldAutoFlagSuspiciousSignal() {
        Signal signal = new Signal(UUID.randomUUID(), "Suspicious", "Desc", "infrastructure", 5, 1, 1, 0, 0.0, null, "NEW", new ArrayList<>(), authorId, LocalDateTime.now());
        
        Signal saved = prioritizationService.saveSignal(signal);
        
        assertEquals("FLAGGED", saved.getStatus());
        assertTrue(saved.getModerationReason().contains("Suspicious"));
    }
}
