package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
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
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityDigestPreparationRepository;
import org.opencivic.signalos.repository.CommunityDigestPublicationRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.DigestScheduleRunRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.WeeklyDigestService;
import org.opencivic.signalos.service.DigestSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The scheduler prepares digests; it does not publish them.
 *
 * <p>A scheduler that sent bulletins on its own would mean a wrong digest reaches residents with
 * nobody accountable for it. That is the question this design answers rather than defers, and these
 * tests pin it: after a run, nothing has been published.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:digestsched;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DigestSchedulerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private DigestSchedulerService schedulerService;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private DigestScheduleRunRepository runRepository;
    @Autowired private CommunityDigestPublicationRepository publicationRepository;
    @Autowired private CommunityDigestPreparationRepository preparationRepository;
    @Autowired private WeeklyDigestService weeklyDigestService;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("sched_coord", "encoded", "sched@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Digest scheduling");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void aRunShouldPrepareADigestAndPublishNothing() {
        signal("Water main break", "utilities", 313.0);

        var report = schedulerService.prepareForAllCommunities(communityId);

        org.junit.jupiter.api.Assertions.assertEquals(1, report.prepared());
        org.junit.jupiter.api.Assertions.assertEquals(0, report.failed());
        org.junit.jupiter.api.Assertions.assertEquals(1, runRepository.count());

        // The whole point: nothing reached residents.
        org.junit.jupiter.api.Assertions.assertEquals(
            0, publicationRepository.count(),
            "the scheduler must not publish; a bulletin reaching residents needs a person accountable for it");
    }

    @Test
    void theReportShouldSayNothingHasReachedResidents() {
        signal("Water main break", "utilities", 313.0);

        var report = schedulerService.prepareForAllCommunities(communityId);

        org.junit.jupiter.api.Assertions.assertTrue(
            report.interpretation().contains("does not publish them"),
            "expected the not-publishing statement, got: " + report.interpretation());
        org.junit.jupiter.api.Assertions.assertTrue(
            report.interpretation().contains("Nothing has reached residents"),
            "expected the nothing-sent statement, got: " + report.interpretation());
        org.junit.jupiter.api.Assertions.assertTrue(
            report.interpretation().contains("nobody accountable for it"),
            "expected the accountability reason, got: " + report.interpretation());
    }

    @Test
    void aSecondRunInTheSameWeekShouldSkipRatherThanDuplicate() {
        signal("Water main break", "utilities", 313.0);

        var first = schedulerService.prepareForAllCommunities(communityId);
        var second = schedulerService.prepareForAllCommunities(communityId);

        org.junit.jupiter.api.Assertions.assertEquals(1, first.prepared());
        // A restart mid-week must not look like two attempts happened.
        org.junit.jupiter.api.Assertions.assertEquals(0, second.prepared());
        org.junit.jupiter.api.Assertions.assertEquals(1, second.skipped());
        org.junit.jupiter.api.Assertions.assertEquals(1, runRepository.count());
    }

    @Test
    void aRunShouldRecordTheWeekItPreparedFor() {
        signal("Water main break", "utilities", 313.0);

        var report = schedulerService.prepareForAllCommunities(communityId);

        // The previous completed week, not the current one.
        org.junit.jupiter.api.Assertions.assertTrue(
            report.weekKey().matches("\\d{4}-W\\d{2}"),
            "expected an ISO week key, got: " + report.weekKey());
        org.junit.jupiter.api.Assertions.assertEquals(1, report.runs().size());
org.junit.jupiter.api.Assertions.assertEquals("PREPARED", report.runs().get(0).outcome());
        // The detail states what was sealed. It must not restate the outcome: the UI renders the
        // outcome next to it, and "waiting for a person to publish" appearing twice read as a stutter.
        org.junit.jupiter.api.Assertions.assertTrue(
            report.runs().get(0).detail().contains("Sealed"),
            "expected the sealed-artifact note, got: " + report.runs().get(0).detail());
        // The thing a reader would otherwise have to assume: publishing sends this artifact, not a
        // fresh recomposition.
        org.junit.jupiter.api.Assertions.assertTrue(
            report.runs().get(0).detail().contains("does not recompose"),
            "expected the artifact guarantee, got: " + report.runs().get(0).detail());
        org.junit.jupiter.api.Assertions.assertTrue(
            report.runs().get(0).detail().contains("item(s), hash"),
            "expected the item count and hash, got: " + report.runs().get(0).detail());
    }

    @Test
    void theHistoryShouldBeReadableThroughTheApi() throws Exception {
        signal("Water main break", "utilities", 313.0);
        schedulerService.prepareForAllCommunities(communityId);

        mockMvc.perform(get("/api/community/weekly-digest/schedule-history")
                .with(user("sched_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].outcome").value("PREPARED"));
    }

    @Test
    void aCommunityWithNoSignalsShouldStillPrepareAnEmptyDigest() {
        // No signals at all. The digest is still generated, because "nothing happened this week" is
        // itself worth telling a community.
        var report = schedulerService.prepareForAllCommunities(communityId);

        org.junit.jupiter.api.Assertions.assertEquals(1, report.prepared());
        org.junit.jupiter.api.Assertions.assertEquals(0, publicationRepository.count());
    }

    @Test
    void theSchedulerShouldBeDisabledByDefault() {
        // Asserted on the real bean, not on a fresh StandardEnvironment. A new StandardEnvironment has
        // none of the application's property sources, so a property read from one returns its default
        // whatever the application is configured to do. That test could not have failed.
        org.junit.jupiter.api.Assertions.assertFalse(
            schedulerService.isSchedulerEnabled(),
            "the scheduler must default to disabled");
    }

    @Test
    void thePreparedDigestShouldBeStoredRatherThanDiscarded() {
        signal("Water main break", "utilities", 313.0);

        schedulerService.prepareForAllCommunities(communityId);

        var preparation = preparationRepository.findByCommunityIdAndWeekKey(
            communityId, weeklyDigestService.resolveWeek(null).key()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(preparation.getBody().isBlank());
        org.junit.jupiter.api.Assertions.assertTrue(
            preparation.getContentHash().matches("[0-9a-f]{64}"),
            "expected a sha-256 hex digest, got: " + preparation.getContentHash());
        org.junit.jupiter.api.Assertions.assertEquals(1, preparation.getItemCount());
        // The item list travels with the artifact, so a preview shows the rows that will be sent.
        org.junit.jupiter.api.Assertions.assertTrue(preparation.getItemsJson().contains("Water main break"));
    }

    @Test
    void publishingShouldSendTheArtifactThatWasPreparedAfterTheWorldChanges() {
        // Two signals so that changing their scores changes the order, which changes the body.
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "utilities", 120.0);

        schedulerService.prepareForAllCommunities(communityId);
        var prepared = weeklyDigestService.digestForPreview(communityId, null, null, "sched_coord");

        // The world moves after preparation: the scores swap, as they would if a coordinator rescored
        // or a formula change was applied.
        rescore("Water main break", 90.0);
        rescore("Streetlight out", 400.0);

        var published = weeklyDigestService.publishDigest(communityId, null, null, "sched_coord");

        org.junit.jupiter.api.Assertions.assertEquals(
            prepared.body(), published.body(),
            "residents must receive the artifact somebody reviewed, not a recomposition of it");
        org.junit.jupiter.api.Assertions.assertEquals(prepared.contentHash(), published.contentHash());

        // And the counterfactual, so the assertion above is not vacuous: recomposing now would differ.
        var recomposed = weeklyDigestService.buildDigestForScheduler(
            communityId, prepared.week().key(), null);
        org.junit.jupiter.api.Assertions.assertNotEquals(
            prepared.body(), recomposed.body(),
            "the world did not actually change the digest; this test is proving nothing");
    }

    @Test
    void aPreviewShouldShowExactlyWhatPublishingWillSend() {
        signal("Water main break", "utilities", 313.0);
        signal("Streetlight out", "utilities", 120.0);

        schedulerService.prepareForAllCommunities(communityId);
        var preview = weeklyDigestService.digestForPreview(communityId, null, null, "sched_coord");

        rescore("Water main break", 90.0);
        rescore("Streetlight out", 400.0);

        // A preview that recomposes while publishing uses the stored artifact would show one digest
        // and send another.
        org.junit.jupiter.api.Assertions.assertEquals(
            preview.body(),
            weeklyDigestService.digestForPreview(communityId, null, null, "sched_coord").body());
        org.junit.jupiter.api.Assertions.assertEquals(
            preview.contentHash(),
            weeklyDigestService.digestForPreview(communityId, null, null, "sched_coord").contentHash());
    }

    @Test
    void aWeekWithNoPreparationShouldStillCompose() {
        // No scheduler involved: a coordinator running the digest by hand still gets one.
        signal("Water main break", "utilities", 313.0);

        var preview = weeklyDigestService.digestForPreview(communityId, null, null, "sched_coord");
        var published = weeklyDigestService.publishDigest(communityId, null, null, "sched_coord");

        org.junit.jupiter.api.Assertions.assertEquals(preview.body(), published.body());
        org.junit.jupiter.api.Assertions.assertEquals(1, publicationRepository.count());
    }

    @Test
    void aSecondPreparationShouldKeepTheArtifactAlreadyUnderReview() {
        signal("Water main break", "utilities", 313.0);

        schedulerService.prepareForAllCommunities(communityId);
        rescore("Water main break", 999.0);
        var second = schedulerService.prepareForAllCommunities(communityId);

        // The week already ran, so this is SKIPPED, not a fresh artifact over the reviewed one.
        org.junit.jupiter.api.Assertions.assertEquals(0, second.prepared());
        org.junit.jupiter.api.Assertions.assertEquals(1, second.skipped());
        org.junit.jupiter.api.Assertions.assertEquals(
            1, preparationRepository.findByCommunityIdAndWeekKey(
                communityId, weeklyDigestService.resolveWeek(null).key()).orElseThrow().getItemCount());
    }

    private void rescore(String title, double score) {
        Signal signal = signalRepository.findAll().stream()
            .filter(candidate -> title.equals(candidate.getTitle()))
            .findFirst()
            .orElseThrow();
        signal.setPriorityScore(score);
        signalRepository.save(signal);
    }

    private void signal(String title, String category, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the scheduler test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        // Inside the previous completed week, so the digest has something to report.
        signal.setCreatedAt(LocalDate.now().minusWeeks(1).atStartOfDay().plusHours(9));
        signalRepository.save(signal);
    }
}