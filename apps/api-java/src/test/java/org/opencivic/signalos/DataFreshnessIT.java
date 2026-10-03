package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityDecisionBasisType;
import org.opencivic.signalos.domain.CommunityDecisionStatus;
import org.opencivic.signalos.domain.CommunityDecisionType;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:freshit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DataFreshnessIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;
    @Autowired private CommunityDecisionRepository decisionRepository;

    private UUID communityId;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        User operator = new User("freshness_op", "encoded", "fresh@example.com", "ROLE_CITIZEN");
        operator.setVerified(true);
        operator.setEnabled(true);
        operatorId = userRepository.save(operator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Freshness monitoring");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(operatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(operatorId);
        membershipRepository.save(membership);
    }

    @Test
    void shouldReportOnEveryMonitoredSurface() throws Exception {
        signalSeries("infrastructure", 8, 7, 2);

        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_op").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].version").value("v1"))
            .andExpect(jsonPath("$[0].communityId").value(communityId.toString()))
            .andExpect(jsonPath("$[0].allHealthy").value(true))
            .andExpect(jsonPath("$[0].staleCount").value(0))
            .andExpect(jsonPath("$[0].sources", hasSize(4)))
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].verdict").value(
                org.hamcrest.Matchers.contains("FRESH")))
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].medianGapDays").value(
                org.hamcrest.Matchers.contains(7)))
            // A community that has never held a decision is a setup question, not a dropout.
            .andExpect(jsonPath("$[0].sources[?(@.key=='DECISIONS')].verdict").value(
                org.hamcrest.Matchers.contains("NO_HISTORY")))
            .andExpect(jsonPath("$[0].guidance").value(
                org.hamcrest.Matchers.containsString("within its usual cadence")));
    }

    @Test
    void shouldFlagAStaleSurfaceAndAdmitTheCauseIsUnknown() throws Exception {
        // Weekly for a while, then nothing for four months.
        signalSeries("utilities", 8, 7, 130);

        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_op").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].allHealthy").value(false))
            // Reports and the status changes made alongside them both go quiet together, so two
            // surfaces flag. That is correct: a broken intake takes out everything it feeds.
            .andExpect(jsonPath("$[0].staleCount").value(2))
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].verdict").value(
                org.hamcrest.Matchers.contains("DORMANT")))
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].cause").value(
                org.hamcrest.Matchers.contains("UNKNOWN")))
            .andExpect(jsonPath("$[0].guidance").value(
                org.hamcrest.Matchers.containsString("confirm the intake path")))
            .andExpect(jsonPath("$[0].guidance").value(
                org.hamcrest.Matchers.containsString("municipal integration")));
    }

    @Test
    void shouldNotFlagACommunityWhoseUsualCadenceIsLong() throws Exception {
        // Same silence, but this place files roughly twice a year.
        signalSeries("infrastructure", 6, 180, 100);

        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_op").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].verdict").value(
                org.hamcrest.Matchers.contains("FRESH")))
            .andExpect(jsonPath("$[0].allHealthy").value(true));
    }

    @Test
    void statusChangesShouldBeMonitoredSeparatelyFromReports() throws Exception {
        signalSeries("infrastructure", 8, 7, 1);
        decisionSeries(6, 30, 200);

        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_op").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sources[?(@.key=='REPORTS')].verdict").value(
                org.hamcrest.Matchers.contains("FRESH")))
            .andExpect(jsonPath("$[0].sources[?(@.key=='DECISIONS')].verdict").value(
                org.hamcrest.Matchers.contains("STALE")));
    }

    @Test
    void outsiderShouldNotSeeAnotherCommunitysFreshness() throws Exception {
        User outsider = new User("freshness_outsider", "encoded", "out@example.com", "ROLE_CITIZEN");
        outsider.setVerified(true);
        outsider.setEnabled(true);
        userRepository.save(outsider);

        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_outsider").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void askingForAnInvisibleCommunityShouldFailLoudly() throws Exception {
        mockMvc.perform(get("/api/community/freshness")
                .with(user("freshness_op").roles("CITIZEN"))
                .queryParam("communityId", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    private void signalSeries(String category, int count, int gapDays, int daysAgo) {
        LocalDate last = LocalDate.now().minusDays(daysAgo);
        for (int i = 0; i < count; i++) {
            UUID signalId = UUID.randomUUID();
            Signal signal = new Signal();
            signal.setId(signalId);
            signal.setCommunityId(communityId);
            signal.setAuthorId(operatorId);
            signal.setTitle("Report " + category + " " + i);
            signal.setDescription("Recorded for the freshness baseline.");
            signal.setCategory(category);
            signal.setStatus("OPEN");
            signal.setUrgency(3);
            signal.setImpact(3);
            signal.setAffectedPeople(40);
            signal.setCommunityVotes(3);
            signal.setPriorityScore(55.0);
            signal.setScoreBreakdown(new ScoreBreakdown(3, 3, 40, 3));
            signal.setLocationLabel("Riverside");
            signal.setCreatedAt(last.minusDays((long) (count - 1 - i) * gapDays).atStartOfDay());
            signalRepository.save(signal);

            SignalStatusEntry entry = new SignalStatusEntry(
                signalId, "OPEN", "OPEN", "freshness_op", "Created for the freshness baseline.");
            entry.setCreatedAt(signal.getCreatedAt());
            statusEntryRepository.save(entry);
        }
    }

    private void decisionSeries(int count, int gapDays, int daysAgo) {
        LocalDate last = LocalDate.now().minusDays(daysAgo);
        for (int i = 0; i < count; i++) {
            CommunityDecision decision = new CommunityDecision();
            decision.setCommunityId(communityId);
            decision.setDecidedBy(operatorId);
            decision.setDecisionType(CommunityDecisionType.APPROVAL);
            decision.setDecisionStatus(CommunityDecisionStatus.RECORDED);
            decision.setApprovalBasisType(CommunityDecisionBasisType.COMMUNITY_VOTE);
            decision.setTitle("Decision " + i);
            decision.setSummary("Recorded for the freshness baseline.");
            decision.setApprovalBasisSummary("Simple majority vote passed.");
            decision.setDecidedAt(last.minusDays((long) (count - 1 - i) * gapDays).atStartOfDay());
            decision.setUpdatedAt(decision.getDecidedAt());
            decisionRepository.save(decision);
        }
    }
}