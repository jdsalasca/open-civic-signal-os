package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.FormulaChangeProposalRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A scoring change proposal is only worth recording if three things hold: the preview actually
 * detects reordering, it refuses to present itself as a forecast, and approving it does not quietly
 * change what runs.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:formulaprop;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FormulaChangeProposalIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private FormulaChangeProposalRepository proposalRepository;

    @BeforeEach
    void setUp() {
        User admin = new User("formula_admin", "encoded", "admin@example.com", "ROLE_SUPER_ADMIN");
        admin.setVerified(true);
        admin.setEnabled(true);
        userRepository.save(admin);
    }

    @Test
    void previewShouldDetectRealReordering() throws Exception {
        // Two signals where the current weights rank A above B, but a votes-heavy formula ranks B above A.
        // A: urgency 5, impact 1, people 10, votes 0  -> 150 + 25 + 1 + 0 = 176 (both weightings)
        // so B overtakes A only under the proposed weights.
        signal("Urgent but unloved", 5, 1, 10, 0, 176.0);
        // B: urgency 1, impact 5, people 10, votes 40 -> 30 + 125 + 1 + 15 = 171 (current),
        signal("Popular but not urgent", 1, 5, 10, 40, 171.0);

        // Weight votes hard: B should overtake A.
        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30,
                      "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10,
                      "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1,
                      "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleSize").value(2))
            .andExpect(jsonPath("$.positionsMoved").value(2))
            .andExpect(jsonPath("$.biggestMovers", hasSize(2)))
            .andExpect(jsonPath("$.currentExpression").value(
                containsString("Urgency * 30")))
            .andExpect(jsonPath("$.proposedExpression").value(
                containsString("Votes/1")));
    }

    @Test
    void previewShouldReportNoMovementWhenTheWeightsAreUnchanged() throws Exception {
        signal("Water main break", 5, 5, 900, 40, 313.0);
        signal("Streetlight out", 3, 3, 60, 4, 106.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30,
                      "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10,
                      "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5,
                      "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.positionsMoved").value(0))
            .andExpect(jsonPath("$.positionsUnchanged").value(2))
            .andExpect(jsonPath("$.movedShare").value(0.0));
    }

    @Test
    void previewShouldRefuseToPresentItselfAsAForecast() throws Exception {
        signal("Water main break", 5, 5, 900, 40, 313.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5, "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isOk())
            // A number like "38% moves" invites a decider to read it as a prediction.
            .andExpect(jsonPath("$.interpretation").value(containsString("does NOT predict the future")))
            .andExpect(jsonPath("$.interpretation").value(containsString("changes what people report")))
            // And it must not pretend to make the judgement.
            .andExpect(jsonPath("$.interpretation").value(containsString("does not say whether the change is")))
            // And it must be explicit that approving is not applying.
            .andExpect(jsonPath("$.interpretation").value(containsString("Approving this proposal does not apply it")));
    }

    @Test
    void aSmallSampleShouldBeCalledOut() throws Exception {
        signal("Water main break", 5, 5, 900, 40, 313.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5, "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interpretation").value(containsString("The sample is only 1")));
    }

    @Test
    void aZeroWeightShouldBeRejectedRatherThanProducingAConfusingPreview() throws Exception {
        signal("Water main break", 5, 5, 900, 40, 313.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 0, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5, "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aNegativeWeightShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": -5, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5, "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void proposingShouldRecordThePreviewTheDeciderWouldSee() throws Exception {
        signal("Urgent but unloved", 5, 1, 10, 0, 176.0);
        signal("Popular but not urgent", 1, 5, 10, 40, 171.0);

        mockMvc.perform(post("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Weight community votes more heavily",
                      "rationale": "Residents report that popular issues lose to single loud complaints.",
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PROPOSED"))
            .andExpect(jsonPath("$.preview.positionsMoved").value(2))
            .andExpect(jsonPath("$.preview.sampleSize").value(2))
            // The evidence a decision is judged against travels with the proposal.
            .andExpect(jsonPath("$.currentWeights.urgencyMultiplier").value(30.0))
            .andExpect(jsonPath("$.proposedWeights.communityVotesCap").value(100.0))
            .andExpect(jsonPath("$.applicationNote").value(
                containsString("does not change the scoring")));
    }

    @Test
    void aProposalWithoutARationaleShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Change things",
                      "rationale": "   ",
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("rationale is required")));
    }

    @Test
    void approvingShouldRecordADecisionAndNotChangeTheScoring() throws Exception {
        signal("Urgent but unloved", 5, 1, 10, 0, 176.0);

        String created = mockMvc.perform(post("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Weight votes more heavily",
                      "rationale": "Popular issues lose to single loud complaints.",
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andReturn().getResponse().getContentAsString();
        String proposalId = created.replaceAll("(?s).*\"proposalId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/formula-change-proposals/{id}/decision", proposalId)
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approve": true, "note": "Agreed at the March assembly."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.decidedAt").isNotEmpty())
            .andExpect(jsonPath("$.decisionNote").value("Agreed at the March assembly."));

        // The formula that runs is untouched: its expression still carries the live constant.
        mockMvc.perform(get("/api/signals/formula"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formula").value(
                containsString("Votes/5")))
            .andExpect(jsonPath("$.formula").value(
                containsString("Urgency * 30")));
    }

    @Test
    void decidingTwiceShouldBeRefused() throws Exception {
        String proposalId = proposeSimple();

        mockMvc.perform(post("/api/formula-change-proposals/{id}/decision", proposalId)
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approve": true, "note": "first"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/formula-change-proposals/{id}/decision", proposalId)
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approve": false, "note": "second thoughts"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("already")));
    }

    @Test
    void thePreviewShouldBePublicButDecidingShouldNotBe() throws Exception {
        // Anyone can ask what a set of weights would do.
        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isOk());

        // Proposing a platform-wide scoring change is not a community-scoped act.
        mockMvc.perform(post("/api/formula-change-proposals")
                .with(user("some_citizen").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Not mine to propose",
                      "rationale": "Citizens cannot change the platform formula.",
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void listingShouldSeparateUndecidedFromDecided() throws Exception {
        String first = proposeSimple();
        proposeSimple();

        mockMvc.perform(post("/api/formula-change-proposals/{id}/decision", first)
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approve": false, "note": "not now"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .queryParam("onlyUndecided", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void aClosedBacklogItemShouldNotAppearInThePreview() throws Exception {
        UUID resolved = signal("Already fixed", 5, 5, 900, 40, 313.0);
        Signal signal = signalRepository.findById(resolved).orElseThrow();
        signal.setStatus("RESOLVED");
        signalRepository.save(signal);

        signal("Still open", 4, 4, 120, 8, 200.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 5, "communityVotesCap": 15
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sampleSize").value(1));
    }

    @Test
    void thePreviewShouldNotStoreAnything() throws Exception {
        signal("Water main break", 5, 5, 900, 40, 313.0);

        mockMvc.perform(post("/api/formula-change-proposals/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.positionsMoved").value(greaterThan(-1)));

        // A preview is a question, not a record. Nothing should have been written.
        org.junit.jupiter.api.Assertions.assertEquals(0, proposalRepository.count());
    }

    private String proposeSimple() throws Exception {
        String body = mockMvc.perform(post("/api/formula-change-proposals")
                .with(user("formula_admin").roles("SUPER_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Weight votes more heavily",
                      "rationale": "Popular issues lose to single loud complaints.",
                      "urgencyMultiplier": 30, "impactMultiplier": 25,
                      "affectedPeopleDivisor": 10, "affectedPeopleCap": 30,
                      "communityVotesDivisor": 1, "communityVotesCap": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return body.replaceAll("(?s).*\"proposalId\":\"([^\"]+)\".*", "$1");
    }

    private UUID signal(String title, int urgency, int impact, int people, int votes, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setAuthorId(null);
        signal.setTitle(title);
        signal.setDescription("Recorded for the formula proposal test.");
        signal.setCategory("infrastructure");
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