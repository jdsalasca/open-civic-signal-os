package org.opencivic.signalos;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
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

/**
 * This feature does not weight anything, and these tests pin that as much as they pin the maths.
 *
 * <p>The platform has no equity data, so an "equity multiplier" would move real priorities based
 * on a number nobody supplied. What it can measure is whether it treats comparable reports
 * inconsistently, and the discipline that keeps that honest is refusing to flag a category it does
 * not have enough reports to judge.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:biasonit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BiasDiagnosticIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;

    private UUID communityId;
    private UUID analystId;

    @BeforeEach
    void setUp() {
        User analyst = new User("bias_analyst", "encoded", "bias@example.com", "ROLE_CITIZEN");
        analyst.setVerified(true);
        analyst.setEnabled(true);
        analystId = userRepository.save(analyst).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Bias diagnostics");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(analystId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(analystId);
        membershipRepository.save(membership);
    }

    @Test
    void shouldFlagACategoryResolvedFarMoreSlowlyThanTheCommunityBaseline() throws Exception {
        // roads: three reports each taking 40 days. parks: three reports each taking 2 days.
        for (int i = 0; i < 3; i++) {
            resolved("Road repair " + i, "roads", 40, 5);
        }
        for (int i = 0; i < 3; i++) {
            resolved("Park bench " + i, "parks", 2, 5);
        }

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v1"))
            .andExpect(jsonPath("$.totalReportsConsidered").value(6))
            .andExpect(jsonPath("$.categories", hasSize(2)))
            .andExpect(jsonPath("$.categories[?(@.category=='roads')].flags[0]")
                .value(contains("SLOWER_RESOLUTION")))
            .andExpect(jsonPath("$.categories[?(@.category=='roads')].medianDaysToResolve")
                .value(contains(40)))
            .andExpect(jsonPath("$.categories[?(@.category=='parks')].flags").value(hasSize(1)))
            .andExpect(jsonPath("$.categories[?(@.category=='parks')].flags[0]").value(hasSize(0)))
            .andExpect(jsonPath("$.disparateCategories").value(contains("roads")));
    }

    @Test
    void shouldRefuseToFlagACategoryWithoutEnoughReports() throws Exception {
        // Two reports averaging 90 days is noise. Flagging it would teach people to ignore flags.
        resolved("Lone road report", "roads", 90, 5);
        resolved("Second road report", "roads", 90, 5);
        for (int i = 0; i < 6; i++) {
            resolved("Park bench " + i, "parks", 2, 5);
        }

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[?(@.category=='roads')].flags[0]")
                .value(contains("NO_BASELINE")))
            .andExpect(jsonPath("$.categoriesWithoutBaseline").value(1))
            .andExpect(jsonPath("$.disparateCategories", hasSize(0)))
            .andExpect(jsonPath("$.scopeStatement").value(
                containsString("too few reports to compare")));
    }

    @Test
    void shouldFlagACategoryWithFarLowerAttention() throws Exception {
        for (int i = 0; i < 4; i++) {
            resolved("Road repair " + i, "roads", 5, 1);
        }
        for (int i = 0; i < 4; i++) {
            resolved("Park bench " + i, "parks", 5, 30);
        }

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[?(@.category=='roads')].flags[0]")
                .value(contains("LOWER_ATTENTION")));
    }

    @Test
    void shouldAlwaysStateWhatItDoesNotMeasure() throws Exception {
        resolved("Road repair", "roads", 5, 3);

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            // A report titled "bias diagnostics" that stayed quiet about having no equity data would
            // invite a reader to conclude it measured fairness across populations. It does not.
            .andExpect(jsonPath("$.scopeStatement").value(
                containsString("does NOT measure fairness across populations")))
            .andExpect(jsonPath("$.scopeStatement").value(
                containsString("no fairness weighting is applied")));
    }

    @Test
    void communityWithoutEnoughReportsShouldDrawNoComparison() throws Exception {
        resolved("Road repair", "roads", 40, 5);
        resolved("Park bench", "parks", 2, 5);

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disparateCategories", hasSize(0)))
            .andExpect(jsonPath("$.categories[?(@.category=='roads')].flags[0]")
                .value(contains("NO_BASELINE")));
    }

    @Test
    void flaggedSignalsShouldBeExcludedFromTheDiagnostic() throws Exception {
        // A moderated report should not distort what the platform is being judged on.
        Signal flagged = new Signal();
        flagged.setId(UUID.randomUUID());
        flagged.setCommunityId(communityId);
        flagged.setAuthorId(analystId);
        flagged.setTitle("Flagged and hidden");
        flagged.setDescription("Removed by a moderator.");
        flagged.setCategory("roads");
        flagged.setStatus("FLAGGED");
        flagged.setPriorityScore(60.0);
        flagged.setScoreBreakdown(new ScoreBreakdown(3, 3, 40, 4));
        flagged.setCreatedAt(LocalDateTime.now().minusDays(400));
        signalRepository.save(flagged);

        for (int i = 0; i < 3; i++) {
            resolved("Road repair " + i, "roads", 5, 3);
        }

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_analyst").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReportsConsidered").value(3));
    }

    @Test
    void formulaEndpointShouldPublishTheThresholds() throws Exception {
        mockMvc.perform(get("/api/signals/bias-diagnostics/formula")
                .with(user("bias_analyst").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minCategoryReports").value(3))
            .andExpect(jsonPath("$.slowerResolutionMultiplier").value(2.0));
    }

    @Test
    void residentWithoutTheScopeShouldNotReadADisparityReport() throws Exception {
        User resident = new User("bias_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(analystId);
        membershipRepository.save(membership);

        mockMvc.perform(get("/api/signals/bias-diagnostics")
                .with(user("bias_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isForbidden());
    }

    private void resolved(String title, String category, int daysToResolve, int votes) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(analystId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the bias diagnostic test.");
        signal.setCategory(category);
        signal.setStatus("RESOLVED");
        signal.setUrgency(3);
        signal.setImpact(3);
        signal.setAffectedPeople(40);
        signal.setCommunityVotes(votes);
        signal.setPriorityScore(60.0);
        signal.setScoreBreakdown(new ScoreBreakdown(3, 3, 40, votes));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.now().minusDays(daysToResolve + 10L));
        signalRepository.save(signal);

        SignalStatusEntry entry = new SignalStatusEntry(
            signal.getId(), "OPEN", "RESOLVED", "bias_analyst", "Resolved for the test.");
        entry.setCreatedAt(LocalDateTime.now().minusDays(10));
        statusEntryRepository.save(entry);
    }
}