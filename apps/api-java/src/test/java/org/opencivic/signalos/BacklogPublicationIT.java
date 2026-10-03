package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
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
import org.opencivic.signalos.repository.CommunityBacklogPublicationRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
 * A published backlog is a claim a visitor can check, so the check has to be honest in both
 * directions: it must notice a real change, and it must not cry wolf over something meaningless.
 *
 * <p>The second half is the one that gets forgotten. Rows with equal scores have no defined order,
 * so hashing their literal order would fail verification for a reason nobody can act on.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:backlogpub;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BacklogPublicationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private CommunityBacklogPublicationRepository publicationRepository;

    private UUID communityId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        User editor = new User("publish_editor", "encoded", "pub@example.com", "ROLE_CITIZEN");
        editor.setVerified(true);
        editor.setEnabled(true);
        editorId = userRepository.save(editor).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Backlog publication");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(editorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(editorId);
        membershipRepository.save(membership);
    }

    @Test
    void publishingShouldRecordAHashAndAFrozenSnapshot() throws Exception {
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);

        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_editor").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Assembly of 12 March"}
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderingHash").isNotEmpty())
            .andExpect(jsonPath("$.itemCount").value(2))
            .andExpect(jsonPath("$.snapshotId").isNotEmpty())
            .andExpect(jsonPath("$.formulaVersion").isNotEmpty())
            .andExpect(jsonPath("$.current").value(true))
            .andExpect(jsonPath("$.matchesLiveRanking").value(true));
    }

    @Test
    void aFreshPublicationShouldVerifyAgainstTheLiveRanking() throws Exception {
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);
        publish();

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(true))
            .andExpect(jsonPath("$.publishedHash").isNotEmpty())
            .andExpect(jsonPath("$.explanation").value(
                containsString("matches what was published")));
    }

    @Test
    void aRescoredSignalShouldBreakTheMatchAndSayWhy() throws Exception {
        UUID first = signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);
        publish();

        // A vote lands. The ranking legitimately changes.
        Signal rescored = signalRepository.findById(first).orElseThrow();
        rescored.setPriorityScore(400.0);
        signalRepository.save(rescored);

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(false))
            // Not actionable if it only says "different".
            .andExpect(jsonPath("$.explanation").value(
                containsString("added, removed, or rescored")))
            // And it must not accuse anyone: a new report is a legitimate change.
            .andExpect(jsonPath("$.explanation").value(
                containsString("not proof of tampering")));
    }

    @Test
    void aNewReportThatEntersThePublishedSetShouldBreakTheMatch() throws Exception {
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);
        publish();

        // Scores high enough to displace an item that was published.
        signal("Abandoned play equipment", "parks", 200.0);

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(false));
    }

    @Test
    void aNewReportBelowThePublishedSetShouldNotBreakTheMatch() throws Exception {
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);
        publish();

        // Ranks below both published items, so the published top-N is unchanged and the claim still
        // holds. Failing here would make the check cry wolf over activity that changed nothing.
        signal("Graffiti on the underpass", "parks", 20.0);

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(true));
    }

    @Test
    void correctingATitleShouldNotBreakTheMatch() throws Exception {
        UUID first = signal("Watermain brak", "utilities", 313.0);
        signal("Streetlight out", "infrastructure", 150.0);
        publish();

        // A typo fix. The ordering claim is unchanged, and a check that failed here would be noise.
        Signal edited = signalRepository.findById(first).orElseThrow();
        edited.setTitle("Water main break");
        signalRepository.save(edited);

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(true));
    }

    @Test
    void publishingAgainShouldSupersedeThePreviousCurrent() throws Exception {
        signal("Water main break", "utilities", 313.0);
        publish();

        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_editor").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Second publication"}
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current").value(true));

        mockMvc.perform(get("/api/community/backlog-publications/current")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.label").value("Second publication"));

        // Exactly one publication is current, enforced by a partial unique index rather than convention.
        long current = publicationRepository.findByCommunityIdOrderByPublishedAtDesc(communityId).stream()
            .filter(publication -> publication.isCurrent())
            .count();
        org.junit.jupiter.api.Assertions.assertEquals(1, current);
    }

    @Test
    void currentAndVerifyShouldBeReadableWithoutASession() throws Exception {
        signal("Water main break", "utilities", 313.0);
        publish();

        // A claim only its author can check is not one a visitor can trust.
        mockMvc.perform(get("/api/community/backlog-publications/current")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderingHash").isNotEmpty());

        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matches").value(true));
    }

    @Test
    void anUnlabelledPublicationShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_editor").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "   "}
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("label is required")));
    }

    @Test
    void aResidentShouldNotPublish() throws Exception {
        User resident = new User("publish_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(editorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_resident").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Not mine to publish"}
                    """.formatted(communityId)))
            .andExpect(status().isForbidden());
    }

    @Test
    void historyShouldListPublicationsNewestFirst() throws Exception {
        signal("Water main break", "utilities", 313.0);
        publish();
        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_editor").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Second"}
                    """.formatted(communityId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/backlog-publications/history")
                .with(user("publish_editor").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void verifyingWithNoPublicationShouldFailLoudly() throws Exception {
        mockMvc.perform(get("/api/community/backlog-publications/verify")
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isNotFound());
    }

    private void publish() throws Exception {
        mockMvc.perform(post("/api/community/backlog-publications")
                .with(user("publish_editor").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Assembly of 12 March"}
                    """.formatted(communityId)))
            .andExpect(status().isOk());
    }

    private UUID signal(String title, String category, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(editorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the backlog publication test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.parse("2026-03-01T09:00:00"));
        return signalRepository.save(signal).getId();
    }
}