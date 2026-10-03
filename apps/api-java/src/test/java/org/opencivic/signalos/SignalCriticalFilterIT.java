package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The CRITICAL filter must be a query, not a client-side highlight.
 *
 * <p>It used to be applied in the browser over the rows already fetched, which meant the count
 * shown to a resident was "how many of the twenty rows I happen to have are critical" rather
 * than "how many critical signals exist". A filter that cannot change the total is a filter that
 * lies about the size of the problem.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:criticalit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalCriticalFilterIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID communityId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        User resident = new User("critical_resident", "encoded", "critical@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        authorId = userRepository.save(resident).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Critical filter");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(authorId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(authorId);
        membershipRepository.save(membership);
    }

    @Test
    void criticalFilterShouldChangeTheQueryNotJustTheHighlight() throws Exception {
        // Three critical, four below the threshold.
        signal("Water main break", 5, 5, 900, 40);
        signal("Unsafe crossing", 5, 5, 600, 30);
        signal("Sewage overflow", 5, 4, 500, 25);
        signal("Faded road marking", 2, 2, 40, 3);
        signal("Loose paving slab", 2, 3, 30, 2);
        signal("Missing bench", 1, 2, 20, 1);
        signal("Graffiti", 1, 1, 10, 0);

        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("minScore", "150")
                .queryParam("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(3)))
            // The total has to reflect the filter, or the resident is told the wrong number.
            .andExpect(jsonPath("$.totalElements").value(3))
            // 5*30 + 5*25 + min(900/10, 30) + min(40/5, 15) = 150 + 125 + 30 + 8.
            .andExpect(jsonPath("$.content[0].priorityScore").value(313.0));
    }

    @Test
    void anAbsentThresholdShouldNotFilterAtAll() throws Exception {
        signal("Water main break", 5, 5, 900, 40);
        signal("Graffiti", 1, 1, 10, 0);

        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void thresholdShouldCombineWithTheStatusFilterRatherThanReplaceIt() throws Exception {
        signal("Water main break", 5, 5, 900, 40);          // critical, NEW
        signal("Faded road marking", 2, 2, 40, 3);            // not critical, NEW
        Signal resolvedCritical = signal("Broken lift", 5, 5, 800, 35); // critical, RESOLVED
        resolvedCritical.setStatus("RESOLVED");
        signalRepository.save(resolvedCritical);

        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("status", "NEW")
                .queryParam("minScore", "150")
                .queryParam("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Water main break"));
    }

    @Test
    void aNegativeThresholdShouldBeRejectedRatherThanSilentlyIgnored() throws Exception {
        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("minScore", "-10"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void thresholdMustNotResurfaceModeratedSignals() throws Exception {
        // High-scoring but moderated. If the threshold path skipped the FLAGGED/REJECTED
        // exclusion, switching a filter on would bring back content a moderator removed.
        Signal flagged = signal("Flagged report", 5, 5, 900, 40);
        flagged.setStatus("FLAGGED");
        signalRepository.save(flagged);
        signal("Sewage overflow", 5, 4, 500, 25);

        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("minScore", "150")
                .queryParam("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Sewage overflow"));
    }

    @Test
    void formulaShouldPublishTheDefaultCriticalThreshold() throws Exception {
        mockMvc.perform(get("/api/signals/meta")
                .with(user("critical_resident").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.criticalScoreThreshold").isNumber());
    }

    @Test
    void topUnresolvedShouldHonourTheSameThresholdContract() throws Exception {
        signal("Water main break", 5, 5, 900, 40);
        signal("Graffiti", 1, 1, 10, 0);

        mockMvc.perform(get("/api/signals/top-10")
                .with(user("critical_resident").roles("CITIZEN"))
                .queryParam("minScore", "150"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Water main break"));
    }

    /**
 * priority_score is a persisted column, and the threshold filter runs against it in SQL, so the
 * fixture has to store the score the real pipeline would have computed. Leaving it at the 0.0
 * default would make every threshold query return nothing and the test would pass for the wrong
 * reason.
 */
private Signal signal(String title, int urgency, int impact, int people, int votes) {
        double score = urgency * 30.0
            + impact * 25.0
            + Math.min(people / 10.0, 30.0)
            + Math.min(votes / 5.0, 15.0);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(authorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the critical filter test.");
        signal.setCategory("infrastructure");
        signal.setStatus("NEW");
        signal.setUrgency(urgency);
        signal.setImpact(impact);
        signal.setAffectedPeople(people);
        signal.setCommunityVotes(votes);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(urgency, impact, people, votes));
        signal.setLocationLabel("Riverside");
        return signalRepository.save(signal);
    }
}