package org.opencivic.signalos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
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

    /** Signals carry a foreign key to their author, so a random id is not a signal - it is an orphan. */
    @Autowired
    private UserRepository userRepository;

    private UUID authorId;

    @BeforeEach
    void createAuthor() {
        User author = new User("dup_author", "{noop}pw", "dup-author@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        authorId = userRepository.save(author).getId();
    }

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
            authorId,
            LocalDateTime.now().minusMinutes(5)
        ));
    }
}
