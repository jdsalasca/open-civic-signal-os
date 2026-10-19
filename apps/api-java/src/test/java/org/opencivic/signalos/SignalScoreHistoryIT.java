package org.opencivic.signalos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.service.PrioritizationService;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalScoreEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalScoreEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Score history, which existed so a report about a closed month can show that month's score.
 *
 * <p>The gap it closes was disclosed rather than hidden: the transparency report carried
 * {@code reproducibilityLimits} saying the score it showed was today's score. This is the other half.
 *
 * <p>Three runtime operations change a score, and all three live in one service: recording a signal, a
 * resident supporting it, and a duplicate merge. Approving a formula change does not rescore anything
 * at runtime, so it is not one of them.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:scorehistory;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignalScoreHistoryIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalScoreEntryRepository scoreEntryRepository;
    @Autowired private PrioritizationService prioritizationService;

    private UUID communityId;
    private UUID authorId;
    private String unique;
    private String authorName;

    @BeforeEach
    void setUp() {
        // Unique per test: this class is not transactional, so rows persist between tests and both
        // username and email are unique columns.
        unique = UUID.randomUUID().toString().substring(0, 8);
        User author = new User("score_author_" + unique, "encoded", "score_" + unique + "@example.com",
            "ROLE_CITIZEN");
        author.setVerified(true);
        author.setEnabled(true);
        authorName = author.getUsername();
        authorId = userRepository.save(author).getId();

        Community community = new Community();
        community.setName("Score History District");
        community.setSlug("score-history-" + unique);
        community.setDescription("Score history");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(authorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(authorId);
        membershipRepository.save(membership);
    }

    @Test
    void recordingASignalShouldStoreItsFirstScore() {
        UUID signalId = signal("Pothole cluster", 4, 4, 120, 0);

        List<SignalScoreEntry> entries = scoreEntryRepository.findBySignalIdOrderByRecordedAtDesc(signalId);

        org.junit.jupiter.api.Assertions.assertEquals(1, entries.size(),
            "the score a signal was born with is part of its history");
        org.junit.jupiter.api.Assertions.assertEquals(SignalScoreEntry.Cause.INGEST, entries.get(0).getCause());
        // Derived from the formula rather than a literal, so this also proves the stored value is the
        // formula's own output and not something recomputed somewhere else.
        org.junit.jupiter.api.Assertions.assertEquals(
            org.opencivic.signalos.domain.PrioritizationFormula.score(4, 4, 120, 0),
            entries.get(0).getPriorityScore(),
            0.001);
    }

    @Test
    void aSupportVoteShouldRecordTheNewScoreAndWhyItMoved() throws Exception {
        UUID signalId = signal("Streetlight out", 4, 4, 120, 0);
        String voter = "voter_" + unique;
        User user = new User(voter, "encoded", voter + "@example.com", "ROLE_CITIZEN");
        user.setVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/signals/{id}/vote", signalId)
                .with(user(voter).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk());

        List<SignalScoreEntry> entries = scoreEntryRepository.findBySignalIdOrderByRecordedAtDesc(signalId);

        // Cause matters: a score that rose because a resident supported the issue is a different fact
        // from one that rose because duplicates were merged.
        org.junit.jupiter.api.Assertions.assertEquals(2, entries.size());
        SignalScoreEntry latest = entries.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(SignalScoreEntry.Cause.SUPPORT_VOTE, latest.getCause());
        org.junit.jupiter.api.Assertions.assertEquals(1, latest.getCommunityVotes(),
            "the recorded history must carry the component, not just the total");
        org.junit.jupiter.api.Assertions.assertTrue(latest.getPriorityScore() > org.opencivic.signalos.domain.PrioritizationFormula.score(4, 4, 120, 0),
            "one more vote should raise the score, got: " + latest.getPriorityScore());
        org.junit.jupiter.api.Assertions.assertEquals("v1", latest.getFormulaVersion());
    }

    @Test
    void theScoreHeldAtAMomentShouldBeReadableAfterTheScoreMovesOn() {
        UUID signalId = signal("Water main break", 4, 4, 120, 0);
        LocalDateTime duringFebruary = LocalDateTime.parse("2026-02-20T09:00:00");
        LocalDateTime afterFebruary = LocalDateTime.parse("2026-04-02T09:00:00");

        // The history a community would really have: one score in February, a higher one in April.
        scoreEntryRepository.save(entry(signalId, org.opencivic.signalos.domain.PrioritizationFormula.score(4, 4, 120, 0), duringFebruary, SignalScoreEntry.Cause.INGEST));
        scoreEntryRepository.save(entry(signalId, 460.0, afterFebruary, SignalScoreEntry.Cause.SUPPORT_VOTE));

        var atFebruaryEnd = scoreEntryRepository.findLatestAtOrBefore(
            signalId, LocalDateTime.parse("2026-03-01T00:00:00").minusNanos(1));
        var atApril = scoreEntryRepository.findLatestAtOrBefore(
            signalId, LocalDateTime.parse("2026-04-30T00:00:00").minusNanos(1));

        org.junit.jupiter.api.Assertions.assertTrue(atFebruaryEnd.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(org.opencivic.signalos.domain.PrioritizationFormula.score(4, 4, 120, 0), atFebruaryEnd.orElseThrow().getPriorityScore(), 0.001,
            "February must show February's score, not the one the issue reached in April");
        org.junit.jupiter.api.Assertions.assertEquals(460.0, atApril.orElseThrow().getPriorityScore(), 0.001,
            "a later moment must see the later score");
    }

    @Test
    void aSignalScoredBeforeThisExistedShouldAnswerEmptyRatherThanGuess() {
        // Legacy data: a row inserted outside the service, so it has a score and no history. This is
        // what every signal predating this feature looks like, and it is the case where guessing
        // would be inventing a past figure rather than reading one.
        Signal legacy = new Signal();
        legacy.setId(UUID.randomUUID());
        legacy.setCommunityId(communityId);
        legacy.setAuthorId(authorId);
        legacy.setTitle("Legacy imported report");
        legacy.setDescription("Imported before score history existed.");
        legacy.setCategory("utilities");
        legacy.setStatus("NEW");
        legacy.setUrgency(3);
        legacy.setImpact(3);
        legacy.setAffectedPeople(40);
        legacy.setCommunityVotes(2);
        legacy.setScoreBreakdown(new org.opencivic.signalos.domain.ScoreBreakdown(3, 3, 40, 2));
        legacy.setPriorityScore(
            org.opencivic.signalos.domain.PrioritizationFormula.score(3, 3, 40, 2));
        UUID legacyId = signalRepository.save(legacy).getId();

        org.junit.jupiter.api.Assertions.assertTrue(
            scoreEntryRepository.findLatestAtOrBefore(legacyId, LocalDateTime.now()).isEmpty(),
            "a signal with no recorded history must answer empty rather than offer today's score as a "
                + "past one; the caller needs to know the difference between 'unrecorded' and 'known'");
    }

    private SignalScoreEntry entry(
        UUID signalId,
        double score,
        LocalDateTime at,
        SignalScoreEntry.Cause cause
    ) {
        SignalScoreEntry entry = new SignalScoreEntry();
        entry.setId(UUID.randomUUID());
        entry.setSignalId(signalId);
        entry.setPriorityScore(score);
        entry.setUrgency(4);
        entry.setImpact(4);
        entry.setAffectedPeople(120);
        entry.setCommunityVotes(8);
        entry.setFormulaVersion("v1");
        entry.setCause(cause);
        entry.setRecordedAt(at);
        return entry;
    }

    /**
     * Creates a signal through the real service, not through the repository.
     *
     * <p>Saving the entity directly bypasses {@code createSignal}, which is where the INGEST entry is
     * recorded — so a repository-made signal correctly has no history, and a test built on one asserts
     * nothing about the production path. Same trap as the messaging connector tests in round 40.
     */
    private UUID signal(String title, int urgency, int impact, int affectedPeople, int votes) {
        Signal created = prioritizationService.createSignal(
            title,
            "Recorded for the score history test.",
            "utilities",
            urgency,
            impact,
            affectedPeople,
            null,
            "Riverside",
            List.of(),
            null,
            null,
            authorName,
            communityId);
        if (votes > 0) {
            throw new IllegalArgumentException(
                "the service creates signals with no votes; use voteForSignal to add them");
        }
        return created.getId();
    }
}