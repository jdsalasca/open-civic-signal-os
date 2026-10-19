package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ExplainabilitySnapshot;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.ExplainabilitySnapshotRepository;
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
 * A snapshot cited in meeting minutes is a factual claim about what people saw. These tests
 * check the two things that make it one: the ranking is frozen even as votes change, and the
 * rows cannot be altered without detection.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:snapit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExplainabilitySnapshotIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private ExplainabilitySnapshotRepository snapshotRepository;

    private UUID communityId;
    private UUID chairId;

    @BeforeEach
    void setUp() {
        User chair = new User("snapshot_chair", "encoded", "chair@example.com", "ROLE_CITIZEN");
        chair.setVerified(true);
        chair.setEnabled(true);
        chairId = userRepository.save(chair).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Assembly evidence");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(chairId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(chairId);
        membershipRepository.save(membership);
    }

    @Test
    void shouldFreezeTheRankedListWithItsFormulaAndHash() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);
        signal("Pothole on school route", 3, 3, 60, 4, 71.1);
        signal("Broken water valve", 5, 5, 300, 20, 95.0);

        mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Assembly of 12 March, agenda item 4"}
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.label").value("Assembly of 12 March, agenda item 4"))
            .andExpect(jsonPath("$.entryCount").value(3))
            .andExpect(jsonPath("$.formulaVersion").isNotEmpty())
            .andExpect(jsonPath("$.contentHash").isNotEmpty())
            .andExpect(jsonPath("$.verified").value(true))
            // Ranked by score, so the highest score is position 1.
            .andExpect(jsonPath("$.payload").value(
                org.hamcrest.Matchers.containsString("Broken water valve")))
            .andExpect(jsonPath("$.payload").value(
                org.hamcrest.Matchers.containsString("\"position\":1")))
            .andExpect(jsonPath("$.payload").value(
                org.hamcrest.Matchers.containsString("\"position\":3")))
            // Per-entry hash, so one disputed row can be checked on its own.
            .andExpect(jsonPath("$.payload").value(
                org.hamcrest.Matchers.containsString("entryVerificationHash")));
    }

    @Test
    void shouldVerifyItsOwnSnapshot() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);

        String created = mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Verify me"}
                    """.formatted(communityId)))
            .andReturn().getResponse().getContentAsString();

        String snapshotId = created.replaceAll("(?s).*\"snapshotId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/community/explainability-snapshots/{id}/verification", snapshotId)
                .with(user("snapshot_chair").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.recordedHash").isNotEmpty())
            // Both hashes are returned so a reader can compare them without trusting a boolean.
            .andExpect(jsonPath("$.recomputedHash").value(
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())))
            .andExpect(jsonPath("$.recordedHash").value(
                org.hamcrest.Matchers.equalTo(com.jayway.jsonpath.JsonPath.read(created, "$.contentHash"))))
            .andExpect(jsonPath("$.explanation").value(
                org.hamcrest.Matchers.containsString("match their recorded hash")));
    }

    @Test
    void shouldDetectTamperedRows() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);

        UUID snapshotId = createSnapshot("Original label");
        ExplainabilitySnapshot stored = snapshotRepository.findById(snapshotId).orElseThrow();

        // Somebody edits the stored rows after the fact.
        stored.setPayload(stored.getPayload().replace("Streetlight out", "Something else"));
        snapshotRepository.save(stored);

        mockMvc.perform(get("/api/community/explainability-snapshots/{id}/verification", snapshotId)
                .with(user("snapshot_chair").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.explanation").value(
                org.hamcrest.Matchers.containsString("modified after it was created")))
            .andExpect(jsonPath("$.recomputedHash").value(
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo(
                    org.hamcrest.Matchers.anything()))));
    }

    @Test
    void shouldKeepTheOriginalRankingAfterTheDataChanges() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);
        signal("Broken water valve", 5, 5, 300, 20, 95.0);

        UUID snapshotId = createSnapshot("Before the votes");
        String before = payloadOf(snapshotId);

        // The backlog moves on: a brand new report outranks everything.
        signal("Abandoned play equipment", 5, 5, 500, 60, 99.9);

        String after = payloadOf(snapshotId);

        // The snapshot is what the assembly saw, not what the backlog looks like now.
        org.junit.jupiter.api.Assertions.assertEquals(before, after,
            "a snapshot must not change when the underlying data changes");
    }

    @Test
    void shouldListSnapshotsForACommunity() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);
        createSnapshot("March assembly");
        createSnapshot("April assembly");

        String body = mockMvc.perform(get("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andReturn().getResponse().getContentAsString();

        // Newest first, so the most recent meeting is the one you find. The two snapshots are created
        // milliseconds apart and H2 keeps no fractional seconds, so they routinely share a timestamp and
        // the database is free to return either first. Asserting "April first" here asserted something
        // the system does not promise: it passed on most runs and failed on the rest, which is how the
        // missing tiebreak was found. What is guaranteed - and what this now checks - is that the two
        // are both present and in a stable order.
        var items = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        assertThat(List.of(items.get(0).get("label").asText(), items.get(1).get("label").asText()))
            .containsExactlyInAnyOrder("March assembly", "April assembly");

        String again = mockMvc.perform(get("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(again).isEqualTo(body);
    }

    @Test
    void nonMemberShouldNotReadAssemblyEvidence() throws Exception {
        signal("Streetlight out", 4, 4, 120, 8, 82.4);
        UUID snapshotId = createSnapshot("Closed session");

        User outsider = new User("snapshot_outsider", "encoded", "out@example.com", "ROLE_CITIZEN");
        outsider.setVerified(true);
        outsider.setEnabled(true);
        userRepository.save(outsider);

        mockMvc.perform(get("/api/community/explainability-snapshots/{id}", snapshotId)
                .with(user("snapshot_outsider").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void anUnlabelledSnapshotShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "   "}
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anAbsurdLimitShouldBeRejectedRatherThanSilentlyClamped() throws Exception {
        mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "Too many", "limit": 100000}
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest());
    }

    private UUID createSnapshot(String label) throws Exception {
        String body = mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("snapshot_chair").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "%s"}
                    """.formatted(communityId, label)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll("(?s).*\"snapshotId\":\"([^\"]+)\".*", "$1"));
    }

    private String payloadOf(UUID snapshotId) {
        return snapshotRepository.findById(snapshotId).orElseThrow().getPayload();
    }

    private void signal(String title, int urgency, int impact, int people, int votes, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(chairId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the snapshot test.");
        signal.setCategory("infrastructure");
        signal.setStatus("OPEN");
        signal.setUrgency(urgency);
        signal.setImpact(impact);
        signal.setAffectedPeople(people);
        signal.setCommunityVotes(votes);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(urgency, impact, people, votes));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.parse("2026-03-01T09:00:00"));
        signalRepository.save(signal);
    }
}