package org.opencivic.signalos;

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
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalMergeDecisionRepository;
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
 * The algorithm suggests, a person decides, and the decision is recorded.
 *
 * <p>Existing duplicate detection and POST /api/signals/merge already merged reports; neither left
 * any record of who decided or what they were shown. These tests pin the three properties that make
 * the difference: the suggestion is stored verbatim, declining is a real outcome, and nothing
 * merges without a named person asking for it.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mergereview;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalMergeReviewIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalMergeDecisionRepository decisionRepository;

    private UUID communityId;
    private UUID moderatorId;
    private UUID targetId;
    private UUID duplicateId;

    @BeforeEach
    void setUp() {
        User moderator = new User("merge_mod", "encoded", "mod@example.com", "ROLE_CITIZEN");
        moderator.setVerified(true);
        moderator.setEnabled(true);
        moderatorId = userRepository.save(moderator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Duplicate review");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(moderatorId);
        membership.setRole(CommunityRole.MODERATOR);
        membership.setCreatedBy(moderatorId);
        membershipRepository.save(membership);

        targetId = signal("Streetlight out on the main corridor", "infrastructure");
        duplicateId = signal("Streetlight out on Main Corridor", "infrastructure");
    }

    @Test
    void suggestionsShouldCarryTheScoreThatJustifiedThem() throws Exception {
        mockMvc.perform(get("/api/signals/merge-review/suggestions")
                .with(user("merge_mod").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].targetSignalId").value(targetId.toString()))
            .andExpect(jsonPath("$[0].candidates", hasSize(1)))
            // A reviewer deciding whether two reports are the same complaint needs to see why the
            // platform thinks so, not just that it does.
            .andExpect(jsonPath("$[0].candidates[0].similarity").isNumber())
            .andExpect(jsonPath("$[0].candidates[0].signalId").value(duplicateId.toString()))
            .andExpect(jsonPath("$[0].threshold").value(0.75));
    }

    @Test
    void approvingShouldRecordWhoDecidedOnWhat() throws Exception {
        mockMvc.perform(post("/api/signals/merge-review/decisions")
                .with(user("merge_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetSignalId": "%s",
                      "decision": "APPROVED",
                      "threshold": 0.75,
                      "note": "Same streetlight, same week, second resident.",
                      "suggestedSimilarities": [
                        {
                          "signalId": "%s",
                          "title": "Streetlight out on Main Corridor",
                          "category": "infrastructure",
                          "status": "NEW",
                          "similarity": 1.0
                        }
                      ]
                    }
                    """.formatted(targetId, duplicateId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").value("APPROVED"))
            .andExpect(jsonPath("$.suggestedCount").value(1))
            // An approval must report a merge that actually happened, not just that it was recorded.
            .andExpect(jsonPath("$.mergedTargetSignalId").isNotEmpty())
            .andExpect(jsonPath("$.decidedAt").isNotEmpty());

        var record = decisionRepository.findAll().get(0);
        org.junit.jupiter.api.Assertions.assertEquals(moderatorId, record.getDecidedBy());
        org.junit.jupiter.api.Assertions.assertEquals(0.75, record.getSimilarityThreshold());
        // The suggestion is stored as shown, not recomputed, so a tweaked threshold next month
        // cannot rewrite what this person was actually shown.
        org.junit.jupiter.api.Assertions.assertTrue(record.getSuggestedSimilarities().contains("1.0"));
    }

    @Test
    void rejectingShouldBeRecordedAndMustNotMerge() throws Exception {
        mockMvc.perform(post("/api/signals/merge-review/decisions")
                .with(user("merge_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetSignalId": "%s",
                      "decision": "REJECTED",
                      "threshold": 0.75,
                      "note": "Different blocks, two separate outages.",
                      "suggestedSimilarities": [
                        {
                          "signalId": "%s",
                          "title": "Streetlight out on Main Corridor",
                          "category": "infrastructure",
                          "status": "NEW",
                          "similarity": 0.8
                        }
                      ]
                    }
                    """.formatted(targetId, duplicateId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decision").value("REJECTED"))
            // Declining is a real outcome, and it must not be reported as a merge.
            .andExpect(jsonPath("$.mergedTargetSignalId").doesNotExist())
            .andExpect(jsonPath("$.suggestedCount").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(1, decisionRepository.count());
    }

    @Test
    void anApprovalBelowItsOwnThresholdShouldBeRefused() throws Exception {
        mockMvc.perform(post("/api/signals/merge-review/decisions")
                .with(user("merge_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetSignalId": "%s",
                      "decision": "APPROVED",
                      "threshold": 0.9,
                      "suggestedSimilarities": [
                        {"signalId": "%s", "title": "t", "category": "infrastructure",
                         "status": "NEW", "similarity": 0.4}
                      ]
                    }
                    """.formatted(targetId, duplicateId)))
            .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(0, decisionRepository.count());
    }

    @Test
    void anApprovalWithNoSuggestionsShouldBeRefusedAsUnauditable() throws Exception {
        mockMvc.perform(post("/api/signals/merge-review/decisions")
                .with(user("merge_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetSignalId": "%s",
                      "decision": "APPROVED",
                      "threshold": 0.75,
                      "suggestedSimilarities": []
                    }
                    """.formatted(targetId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(org.hamcrest.Matchers.containsString("unauditable")));
    }

    @Test
    void historyShouldExposeBothApprovalsAndRejections() throws Exception {
        decide("APPROVED", 1.0, "first");
        decide("REJECTED", 0.8, "second");

        mockMvc.perform(get("/api/signals/merge-review/history")
                .with(user("merge_mod").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            // Newest first, so the current state of a target is the first thing read.
            .andExpect(jsonPath("$[0].decision").value("REJECTED"))
            .andExpect(jsonPath("$[1].decision").value("APPROVED"));
    }

    @Test
    void aResidentShouldNotReviewOrSeeTheQueue() throws Exception {
        User resident = new User("merge_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(moderatorId);
        membershipRepository.save(membership);

        mockMvc.perform(get("/api/signals/merge-review/suggestions")
                .with(user("merge_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isForbidden());
    }

    private void decide(String decision, double similarity, String note) throws Exception {
        mockMvc.perform(post("/api/signals/merge-review/decisions")
                .with(user("merge_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetSignalId": "%s",
                      "decision": "%s",
                      "threshold": 0.5,
                      "note": "%s",
                      "suggestedSimilarities": [
                        {"signalId": "%s", "title": "t", "category": "infrastructure",
                         "status": "NEW", "similarity": %s}
                      ]
                    }
                    """.formatted(targetId, decision, note, duplicateId, similarity)))
            .andExpect(status().isOk());
    }

    private UUID signal(String title, String category) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(moderatorId);
        signal.setTitle(title);
        signal.setDescription("Reported for the duplicate review test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(3);
        signal.setImpact(3);
        signal.setAffectedPeople(40);
        signal.setCommunityVotes(4);
        signal.setPriorityScore(60.0);
        signal.setScoreBreakdown(new ScoreBreakdown(3, 3, 40, 4));
        signal.setLocationLabel("Main corridor");
        signal.setCreatedAt(LocalDateTime.parse("2026-03-01T09:00:00"));
        return signalRepository.save(signal).getId();
    }
}