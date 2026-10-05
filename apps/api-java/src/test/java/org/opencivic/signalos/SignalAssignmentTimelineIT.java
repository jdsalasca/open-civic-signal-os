package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:signalassignmenttimelineitdb;DB_CLOSE_DELAY=-1")
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
    @Autowired
    private SignalStatusEntryRepository statusEntryRepository;
    @Autowired
    private EntityManager entityManager;

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

    /**
     * The sequence column is the whole point of V50, and under the suite it was never populated.
     *
     * <p>{@code ddl-auto: create-drop} replaces whatever Flyway built, so the identity column declared in
     * V50 does not exist in the test schema - only what the entity maps. Mapped as
     * {@code insertable = false}, Hibernate never wrote to it, so every row had {@code seq = NULL} and
     * {@code order by ... coalesce(e.seq, 0) desc} quietly degraded to ordering by timestamp alone: the
     * exact defect V50 was added to fix, invisible to every test in the suite.
     */
    @Test
    @WithMockUser(username = "staff", roles = {"PUBLIC_SERVANT"})
    void everyStatusEventShouldCarryTheSequenceTheTimelineOrdersBy() throws Exception {
        mockMvc.perform(patch("/api/signals/{id}/assign", signalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assigneeUsername":"liaison",
                      "reason":"Transit liaison will coordinate field verification"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/signals/{id}/status", signalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status":"IN_PROGRESS",
                      "reason":"Field verification started"
                    }
                    """))
            .andExpect(status().isOk());

        // Read the rows back from the database rather than trusting the instances this test just
        // persisted. seq is insertable = false, so the column is assigned by the database and the
        // managed objects still in the persistence context carry null for it - reading those would test
        // Hibernate's in-memory state instead of the claim being made here.
        entityManager.flush();
        entityManager.clear();

        var timeline = statusEntryRepository.findTimeline(signalId);
        assertThat(timeline).isNotEmpty();
        for (var event : timeline) {
            assertThat(event.getSeq())
                .as("status event %s has no insertion sequence, so the timeline is ordering by "
                    + "timestamp alone - the defect V50 exists to prevent", event.getId())
                .isNotNull();
        }

        // And the sequence is not merely present: it is monotonic in the order the events happened, which
        // is what "newest first" means when two events share a timestamp. With createdAt alone the two
        // transitions above are one click apart and H2 keeps no fractional seconds, so the database was
        // free to return them either way round.
        var sequences = timeline.stream().map(SignalStatusEntry::getSeq).toList();
        assertThat(sequences).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(sequences.get(0))
            .as("the later event must carry the higher sequence")
            .isGreaterThan(sequences.get(1));
    }
}
