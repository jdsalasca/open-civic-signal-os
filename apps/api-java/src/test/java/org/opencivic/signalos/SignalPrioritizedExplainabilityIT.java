package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signalprioritizedexplainabilityitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalPrioritizedExplainabilityIT {

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
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void prioritizedListShouldExposeExplainabilitySummary() throws Exception {
        signalRepository.deleteAll();

        signalRepository.save(new Signal(
            UUID.randomUUID(),
            "Flooded avenue after rain",
            "Drainage collapse blocks mobility and damages homes.",
            "infrastructure",
            5,
            4,
            70,
            20,
            0.0,
            new ScoreBreakdown(150, 100, 7, 4),
            "NEW",
            new ArrayList<>(),
            authorId,
            LocalDateTime.now().minusMinutes(15)
        ));

        mockMvc.perform(get("/api/signals/prioritized?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Flooded avenue after rain"))
            .andExpect(jsonPath("$.content[0].explainabilitySummary.version").value("v1"))
            .andExpect(jsonPath("$.content[0].explainabilitySummary.topFactors[0].key").value("urgency"));
    }
}
