package org.opencivic.signalos;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import org.opencivic.signalos.repository.CommunityDigestPublicationRepository;
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
 * A digest for a past week must reproduce exactly, and publishing it twice must be impossible.
 *
 * <p>Both properties exist because of failures this project has already had: the ranking script
 * that could not detect drift, and the filter that reported the wrong count. A bulletin whose
 * content changes between regenerations cannot be cited by a resident, and a bulletin delivered
 * twice teaches people to ignore the channel.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:digestit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WeeklyDigestIT {

    /** A Monday, so the ISO week is unambiguous. */
    private static final String WEEK = "2026-W13";
    private static final LocalDate MONDAY = LocalDate.parse("2026-03-23");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;
    @Autowired private CommunityDigestPublicationRepository publicationRepository;

    private UUID communityId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        User editor = new User("digest_editor", "encoded", "digest@example.com", "ROLE_CITIZEN");
        editor.setVerified(true);
        editor.setEnabled(true);
        editorId = userRepository.save(editor).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Weekly digest");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(editorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(editorId);
        membershipRepository.save(membership);
    }

    @Test
    void shouldListTheTopUnresolvedWithAReasonForEachPosition() throws Exception {
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));
        signal("Streetlight out", "infrastructure", 150.0, MONDAY.plusDays(2));
        signal("Loose paving slab", "infrastructure", 90.0, MONDAY.minusDays(20));

        mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v1"))
            .andExpect(jsonPath("$.week.key").value(WEEK))
            .andExpect(jsonPath("$.week.startDate").value("2026-03-23"))
            .andExpect(jsonPath("$.week.endDate").value("2026-03-30"))
            .andExpect(jsonPath("$.week.previousKey").value("2026-W12"))
            .andExpect(jsonPath("$.topUnresolved", hasSize(3)))
            // Highest priority first.
            .andExpect(jsonPath("$.topUnresolved[0].title").value("Water main break"))
            // AGENTS.md: every list exposes why an item is ranked where it is.
            .andExpect(jsonPath("$.topUnresolved[0].whyRanked").value(
                containsString("urgency")))
            .andExpect(jsonPath("$.topUnresolved[0].whyRanked").value(
                containsString("people affected")))
            .andExpect(jsonPath("$.reportedThisWeek").value(2));
    }

    @Test
    void regeneratingTheSameWeekShouldProduceTheSameBodyAndHash() throws Exception {
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));
        signal("Streetlight out", "infrastructure", 150.0, MONDAY.plusDays(2));

        String first = bodyOf(WEEK);
        String second = bodyOf(WEEK);

        org.junit.jupiter.api.Assertions.assertEquals(first, second,
            "a digest for a past week must reproduce exactly");
    }

    @Test
    void aClosedWeekShouldNotChangeWhenLaterReportsArrive() throws Exception {
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));
        String before = bodyOf(WEEK);

        // A report filed well after the reported week must not alter that week's digest.
        signal("Later road problem", "roads", 300.0, MONDAY.plusDays(30));

        org.junit.jupiter.api.Assertions.assertEquals(before, bodyOf(WEEK),
            "a snapshot of a past week must not absorb later activity");
    }

    @Test
    void publishingTwiceForTheSameWeekShouldBeRefused() throws Exception {
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.published").value(true))
            .andExpect(jsonPath("$.publishedAt").isNotEmpty())
            .andExpect(jsonPath("$.contentHash").isNotEmpty());

        // Residents must not receive the same bulletin twice.
        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("already published")));

        org.junit.jupiter.api.Assertions.assertEquals(1, publicationRepository.count());
    }

    @Test
    void historyShouldRecordWhatWasSentAndItsSealedBody() throws Exception {
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));
        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/weekly-digest/history")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].weekKey").value(WEEK))
            // The rendered body is kept, so a resident can compare what they received.
            .andExpect(jsonPath("$[0].body").value(containsString("weekly digest 2026-W13")))
            .andExpect(jsonPath("$[0].contentHash").isNotEmpty());
    }

    @Test
    void aMalformedWeekShouldBeRejectedRatherThanGuessedAt() throws Exception {
        mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", "last-week"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anAbsurdLimitShouldBeRejectedRatherThanClamped() throws Exception {
        mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK)
                .queryParam("limit", "5000"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void resolvedAndRejectedShouldBeCountedSeparately() throws Exception {
        UUID resolved = signal("Fixed streetlight", "infrastructure", 100.0, MONDAY.minusDays(3));
        addStatus(resolved, "RESOLVED", MONDAY.plusDays(1));
        UUID rejected = signal("Duplicate report", "infrastructure", 20.0, MONDAY.minusDays(3));
        addStatus(rejected, "REJECTED", MONDAY.plusDays(2));
        signal("Water main break", "utilities", 220.0, MONDAY.plusDays(1));

        mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolvedThisWeek").value(1))
            // A rejection is a legitimate outcome, but it is not a civic win.
            .andExpect(jsonPath("$.rejectedThisWeek").value(1))
            // Resolved items are not in the unresolved list.
            .andExpect(jsonPath("$.topUnresolved[?(@.title=='Fixed streetlight')]", hasSize(0)));
    }

    @Test
    void residentWithoutTheScopeShouldNotGenerateOrPublish() throws Exception {
        User resident = new User("digest_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(editorId);
        membershipRepository.save(membership);

        mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isForbidden());
    }

    private String bodyOf(String week) throws Exception {
        String json = mockMvc.perform(get("/api/community/weekly-digest")
                .with(user("digest_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", week))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.body").toString();
    }

    private UUID signal(String title, String category, double score, LocalDate createdAt) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(editorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the digest test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(createdAt.atStartOfDay().plusHours(9));
        return signalRepository.save(signal).getId();
    }

    private void addStatus(UUID signalId, String to, LocalDate at) {
        SignalStatusEntry entry = new SignalStatusEntry(
            signalId, "NEW", to, "digest_editor", "Recorded for the digest test.");
        entry.setCreatedAt(at.atStartOfDay().plusHours(10));
        statusEntryRepository.save(entry);
    }
}