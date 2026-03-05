package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalAssignmentTimelineIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignalRepository signalRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID signalId;

    @BeforeEach
    void setUp() {
        User reporter = new User("reporter", "{noop}pw", "reporter@test.dev", "ROLE_CITIZEN");
        reporter.setEnabled(true);
        reporter.setVerified(true);
        reporter = userRepository.save(reporter);

        User staff = new User("staff", "{noop}pw", "staff@test.dev", "ROLE_PUBLIC_SERVANT");
        staff.setEnabled(true);
        staff.setVerified(true);
        userRepository.save(staff);

        User assignee = new User("liaison", "{noop}pw", "liaison@test.dev", "ROLE_PUBLIC_SERVANT");
        assignee.setEnabled(true);
        assignee.setVerified(true);
        userRepository.save(assignee);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("Bus lane drainage failure");
        signal.setDescription("Standing water blocks buses after every storm.");
        signal.setCategory("mobility");
        signal.setStatus("NEW");
        signal.setAuthorId(reporter.getId());
        signal.setCreatedAt(LocalDateTime.now().minusHours(2));
        signal = signalRepository.save(signal);
        signalId = signal.getId();
    }

    @Test
    @WithMockUser(username = "staff", roles = {"PUBLIC_SERVANT"})
    void assignmentAndStatusTransitionsShouldAppearInTimeline() throws Exception {
        mockMvc.perform(patch("/api/signals/{id}/assign", signalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assigneeUsername":"liaison",
                      "reason":"Transit liaison will coordinate field verification"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedToUsername").value("liaison"));

        mockMvc.perform(patch("/api/signals/{id}/status", signalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status":"IN_PROGRESS",
                      "reason":"Field verification started"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/signals/{id}/history", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").value("STATUS_CHANGED"))
            .andExpect(jsonPath("$[0].changedBy").value("staff"))
            .andExpect(jsonPath("$[0].reason").value("Field verification started"))
            .andExpect(jsonPath("$[1].eventType").value("ASSIGNED"))
            .andExpect(jsonPath("$[1].assignedToUsername").value("liaison"));
    }
}
