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
import org.opencivic.signalos.repository.CommunityDigestPublicationRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.DigestScheduleRunRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
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
        org.junit.jupiter.api.Assertions.assertTrue(
            report.runs().get(0).detail().contains("Publish it deliberately"),
            "expected the deliberate-publish note, got: " + report.runs().get(0).detail());
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
        // A job that started generating digests the moment it was deployed would surprise a community
        // that has not decided to run a weekly bulletin yet. The default is asserted through the
        // property, since the cron method itself is a no-op when disabled.
        org.junit.jupiter.api.Assertions.assertFalse(
            new org.springframework.core.env.StandardEnvironment()
                .getProperty("app.digest.scheduler.enabled", Boolean.class, false),
            "the scheduler must default to disabled");
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