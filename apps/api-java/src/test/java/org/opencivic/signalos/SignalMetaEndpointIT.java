package org.opencivic.signalos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signalmetaendpointitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SignalMetaEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignalRepository signalRepository;

    @Autowired
    private UserRepository userRepository;

    /** Signals carry a foreign key to their author, so a random id is not a signal - it is an orphan. */
    private UUID authorId;

    @BeforeEach
    void createAuthor() {
        User author = new User("signal_author", "{noop}pw", "signal-author@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        authorId = userRepository.save(author).getId();
    }

    @Test
    void shouldReturnMetaWithCountsAndLastUpdated() throws Exception {
        signalRepository.deleteAll();

        signalRepository.save(new Signal(
            UUID.randomUUID(),
            "Streetlight outage",
            "Main avenue remains dark at night.",
            "infrastructure",
            4,
            4,
            120,
            5,
            0.0,
            new ScoreBreakdown(120, 100, 12, 1),
            "NEW",
            new ArrayList<>(),
            authorId,
            LocalDateTime.now().minusHours(3)
        ));

        signalRepository.save(new Signal(
            UUID.randomUUID(),
            "Pothole fixed",
            "The reported pothole was resolved.",
            "infrastructure",
            2,
            2,
            40,
            2,
            0.0,
            new ScoreBreakdown(60, 50, 4, 0.4),
            "RESOLVED",
            new ArrayList<>(),
            authorId,
            LocalDateTime.now().minusHours(1)
        ));

        mockMvc.perform(get("/api/signals/meta"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSignals").value(2))
            .andExpect(jsonPath("$.unresolvedSignals").value(1))
            .andExpect(jsonPath("$.lastUpdatedAt").exists());
    }
}
