package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A sandbox is not a diff. The preview says how many positions move; this says which categories
 * gain and which lose, because that is the policy consequence a decision should be argued about.
 *
 * <p>And it must not rank the scenarios. There is no best weight set, because best depends on what a
 * community wants to prioritise, which is a value judgement the platform has no standing to make.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:policysim;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PolicySimulationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SignalRepository signalRepository;

    @BeforeEach
    void setUp() {
        // Two categories with opposite profiles, so a weight change favours one over the other.
        // utilities: high urgency, low votes. infrastructure: low urgency, high votes.
        for (int i = 0; i < 3; i++) {
            signal("Water outage " + i, "utilities", 5, 1, 10, 0, 176.0);
        }
        for (int i = 0; i < 3; i++) {
            signal("Faded road marking " + i, "infrastructure", 1, 5, 10, 40, 171.0);
        }
    }

    @Test
    void shouldReportWhichCategoryGainsAndWhichLoses() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "Votes-heavy",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleSize").value(6))
            .andExpect(jsonPath("$.scenarios", hasSize(1)))
            .andExpect(jsonPath("$.scenarios[0].name").value("Votes-heavy"))
            .andExpect(jsonPath("$.scenarios[0].categoryShifts", hasSize(2)))
            // The finding is the shift, not the count.
            .andExpect(jsonPath("$.scenarios[0].reading").value(
                containsString("infrastructure gains")))
            .andExpect(jsonPath("$.scenarios[0].reading").value(
                containsString("utilities loses")));
    }

    @Test
    void shouldCompareSeveralScenariosAgainstTheSameBacklog() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "Votes-heavy",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        },
                        {
                          "name": "Urgency-heavy",
                          "urgencyMultiplier": 100, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 5, "communityVotesCap": 15
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarios", hasSize(2)))
            .andExpect(jsonPath("$.scenarios[0].name").value("Votes-heavy"))
            .andExpect(jsonPath("$.scenarios[1].name").value("Urgency-heavy"))
            // Same sample for both, so the comparison is like for like.
            .andExpect(jsonPath("$.sampleSize").value(6));
    }

    @Test
    void shouldRefuseToRankTheScenarios() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "A",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            // There is no best weight set, and the response must not imply one.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does NOT rank the scenarios")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("value judgement the platform has no standing to make")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does not predict the future")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("a simulation is a question, not a decision")));
    }

    @Test
    void aScenarioWithoutANameShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "   ",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("needs a name")));
    }

    @Test
    void tooManyScenariosShouldBeRejectedRatherThanTruncated() throws Exception {
        StringBuilder scenarios = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) {
                scenarios.append(",");
            }
            scenarios.append("""
                {
                  "name": "S%d",
                  "urgencyMultiplier": 30, "impactMultiplier": 25,
                  "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                  "communityVotesDivisor": 1, "communityVotesCap": 100
                }
                """.formatted(i));
        }

        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scenarios\": [" + scenarios + "]}"))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("At most 5 scenarios")));
    }

    @Test
    void anEmptyScenarioListShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scenarios\": []}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aZeroWeightShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "Broken",
                          "urgencyMultiplier": 0, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        }
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aScenarioIdenticalToTheCurrentWeightsShouldMoveNothing() throws Exception {
        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "No change",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 5, "communityVotesCap": 15
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scenarios[0].positionsMoved").value(0))
            .andExpect(jsonPath("$.scenarios[0].movedShare").value(0.0));
    }

    @Test
    void closedSignalsShouldNotAppearInTheSimulation() throws Exception {
        UUID resolved = signal("Already fixed", "utilities", 5, 5, 900, 40, 313.0);
        Signal signal = signalRepository.findById(resolved).orElseThrow();
        signal.setStatus("RESOLVED");
        signalRepository.save(signal);

        mockMvc.perform(post("/api/policy-simulation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scenarios": [
                        {
                          "name": "A",
                          "urgencyMultiplier": 30, "impactMultiplier": 25,
                          "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                          "communityVotesDivisor": 1, "communityVotesCap": 100
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleSize").value(6));
    }

    private UUID signal(
        String title, String category, int urgency, int impact, int people, int votes, double score
    ) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle(title);
        signal.setDescription("Recorded for the policy simulation test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(urgency);
        signal.setImpact(impact);
        signal.setAffectedPeople(people);
        signal.setCommunityVotes(votes);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(urgency, impact, people, votes));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.parse("2026-03-01T09:00:00"));
        return signalRepository.save(signal).getId();
    }
}