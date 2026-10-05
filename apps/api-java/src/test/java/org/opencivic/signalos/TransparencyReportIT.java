package org.opencivic.signalos;

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
 * A transparency report gets published, quoted in a council session, and challenged months
 * later. That only works if regenerating it yields the same numbers, so determinism is the
 * property under test rather than a nice-to-have.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reportit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransparencyReportIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;
    @Autowired private org.opencivic.signalos.repository.SignalScoreEntryRepository scoreEntryRepository;
    @Autowired private org.opencivic.signalos.service.DigestSchedulerService schedulerService;

    private UUID communityId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("report_coord", "encoded", "report@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        authorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Monthly transparency");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(authorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(authorId);
        membershipRepository.save(membership);
    }

    @Test
    void closedMonthShouldReportReportedResolvedAndStillOpen() throws Exception {
        // February 2026: three reported, one resolved on the 20th, two still open.
        signal("Streetlight out", "2026-02-03T09:00:00", "OPEN", 70.0);
        UUID resolved = signal("Pothole on school route", "2026-02-05T09:00:00", "RESOLVED", 82.0);
        signal("Broken water valve", "2026-02-11T09:00:00", "IN_PROGRESS", 64.0);
        addStatus(resolved, "OPEN", "RESOLVED", LocalDateTime.parse("2026-02-20T09:00:00"));

        // January: one reported, one resolved, for the comparison column.
        UUID januaryResolved = signal("Graffiti on underpass", "2026-01-08T09:00:00", "RESOLVED", 55.0);
        addStatus(januaryResolved, "OPEN", "RESOLVED", LocalDateTime.parse("2026-01-15T09:00:00"));

        mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", "2026-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityName").value("Riverside District"))
            .andExpect(jsonPath("$.period.key").value("2026-02"))
            .andExpect(jsonPath("$.period.startDate").value("2026-02-01"))
            .andExpect(jsonPath("$.period.endDate").value("2026-03-01"))
            .andExpect(jsonPath("$.period.previous.key").value("2026-01"))
            .andExpect(jsonPath("$.formulaVersion").isNotEmpty())
            .andExpect(jsonPath("$.metrics", hasSize(7)))
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_REPORTED')].value").value(
                org.hamcrest.Matchers.contains(3)))
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_RESOLVED')].value").value(
                org.hamcrest.Matchers.contains(1)))
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_STILL_OPEN')].value").value(
                org.hamcrest.Matchers.contains(2)))
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_REPORTED')].previousValue").value(
                org.hamcrest.Matchers.contains(1)))
            // January resolution took 7 days, so the delta is negative: February was faster.
            .andExpect(jsonPath("$.metrics[?(@.key=='MEDIAN_RESOLUTION_DAYS')].value").value(
                org.hamcrest.Matchers.contains(15)))
            .andExpect(jsonPath("$.metrics[?(@.key=='MEDIAN_RESOLUTION_DAYS')].direction").value(
                org.hamcrest.Matchers.contains("UP")))
            .andExpect(jsonPath("$.actioned", hasSize(1)))
            .andExpect(jsonPath("$.actioned[0].daysOpen").value(15))
            .andExpect(jsonPath("$.unaddressed", hasSize(2)))
            .andExpect(jsonPath("$.narrative").isNotEmpty());
    }

    @Test
    void theReportMustSayThatScoresAreNotReproducible() throws Exception {
        // Round 46 disclosed the gap because the record's javadoc claimed "regenerating the same month
        // later yields the same figures", which was false for scores. Round 48 added the ledger, so the
        // disclosure now has to describe the narrower remaining case rather than the old one: signals
        // recorded before score history existed.
        signal("Playground fence", "2026-02-06T09:00:00", "OPEN", 40.0);

        mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", "2026-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reproducibilityLimits", org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThan(0))))
            .andExpect(jsonPath("$.reproducibilityLimits[0]").value(
                org.hamcrest.Matchers.containsString("ledger")))
            .andExpect(jsonPath("$.reproducibilityLimits[0]").value(
                org.hamcrest.Matchers.containsString("before score history existed")));
    }

    @Test
    void regeneratingTheSameMonthShouldProduceIdenticalFigures() throws Exception {
        signal("Streetlight out", "2026-02-03T09:00:00", "OPEN", 70.0);
        UUID resolved = signal("Pothole on school route", "2026-02-05T09:00:00", "RESOLVED", 82.0);
        addStatus(resolved, "OPEN", "RESOLVED", LocalDateTime.parse("2026-02-20T09:00:00"));

        String first = getReport("2026-02");
        String second = getReport("2026-02");

        // The only field allowed to differ is generatedAt, which describes the run.
        org.junit.jupiter.api.Assertions.assertEquals(
            stripGeneratedAt(first), stripGeneratedAt(second),
            "a regenerated report must be byte-identical once generatedAt is excluded");
    }

    @Test
    void daysOpenForUnresolvedSignalsShouldNotDriftWithTheWallClock() throws Exception {
        // Reported in February and never resolved. Measuring to today would make this figure
        // change on every regeneration; measuring to period end keeps it stable.
        signal("Abandoned play equipment", "2026-02-02T09:00:00", "OPEN", 40.0);

        String report = getReport("2026-02");
        // Reported 2 February, measured to the period end of 28 February.
        org.junit.jupiter.api.Assertions.assertTrue(
            report.contains("\"daysOpen\":26"),
            "expected 26 days to the February period end, got: " + report);
    }

    @Test
    void aClosedMonthShouldNotRewriteItselfWhenIssuesAreSettledAfterItEnded() throws Exception {
        // Two issues reported in February. One is resolved on 20 February, inside the period. The other
        // is resolved in April, after the period closed.
        UUID resolvedInFebruary = signal("Pothole cluster", "2026-02-05T09:00:00", "OPEN", 120.0);
        UUID resolvedInApril = signal("Playground fence", "2026-02-06T09:00:00", "RESOLVED", 90.0);
        addStatus(resolvedInFebruary, "OPEN", "RESOLVED", LocalDateTime.parse("2026-02-20T09:00:00"));
        addStatus(resolvedInApril, "OPEN", "RESOLVED", LocalDateTime.parse("2026-04-02T09:00:00"));

        String report = getReport("2026-02");

        // February's unaddressed list is a claim about February. The fence was unaddressed in
        // February, so it belongs there even though April has since fixed it.
        org.junit.jupiter.api.Assertions.assertTrue(
            report.contains("Playground fence"),
            "an issue settled after the period ended must still appear in that period's unaddressed "
                + "list; otherwise the record of what a community failed to address in February "
                + "rewrites itself every time it does the work later: " + report);
        // And the one resolved inside the period does not.
        org.junit.jupiter.api.Assertions.assertFalse(
            unaddressedSection(report).contains("Pothole cluster"),
            "an issue resolved inside the period is not unaddressed: " + unaddressedSection(report));
    }

    @Test
    void anIssueSettledBeforeThePeriodShouldNotAppearAsUnaddressedInIt() throws Exception {
        UUID resolvedInJanuary = signal("Streetlight out", "2026-01-20T09:00:00", "RESOLVED", 70.0);
        addStatus(resolvedInJanuary, "OPEN", "RESOLVED", LocalDateTime.parse("2026-01-25T09:00:00"));

        org.junit.jupiter.api.Assertions.assertFalse(
            unaddressedSection(getReport("2026-02")).contains("Streetlight out"),
            "an issue settled before the period started was never unaddressed during it");
    }

    /** The unaddressed list only, so an assertion cannot be satisfied by the actioned list instead. */
    /**
 * The count and the list in one report must answer the same question.
 *
 * <p>They used not to. {@code SIGNALS_STILL_OPEN} counted from the audit trail, bounded by the period
 * end, while the {@code unaddressed} list filtered on the live status column. So February's report said
 * "1 still open" in its metrics and listed zero unaddressed issues — a report disagreeing with itself,
 * with the number and the detail published side by side.
 */
@Test
void theStillOpenCountAndTheUnaddressedListMustAgree() throws Exception {
    UUID resolvedInside = signal("Pothole cluster", "2026-02-05T09:00:00", "OPEN", 120.0);
    signal("Playground fence", "2026-02-06T09:00:00", "OPEN", 90.0);
    signal("Streetlight out", "2026-02-07T09:00:00", "OPEN", 60.0);
    addStatus(resolvedInside, "OPEN", "RESOLVED", LocalDateTime.parse("2026-02-20T09:00:00"));

    String report = getReport("2026-02");

    int metric = Integer.parseInt(report.replaceAll("(?s).*SIGNALS_STILL_OPEN.*?\"value\":(\\d+).*", "$1"));
    int listed = unaddressedSection(report).split("\"signalId\"").length - 1;

    org.junit.jupiter.api.Assertions.assertEquals(2, metric,
        "two issues were reported in February and settled after it ended: " + report);
    org.junit.jupiter.api.Assertions.assertEquals(metric, listed,
        "the metric and the list in the same report must not disagree: " + report);
}

private String unaddressedSection(String report) {
        int start = report.indexOf("\"unaddressed\"");
        if (start < 0) {
            return "";
        }
        int end = report.indexOf("]", start);
        return end < 0 ? report.substring(start) : report.substring(start, end);
    }

    @Test
    void aClosedMonthShouldShowTheScoreItHeldNotTheScoreTheIssueReachedLater() throws Exception {
        // This is the gap round 46 disclosed in the payload rather than hid. The report used to show
        // today's score for a past period, because priorityScore is mutable and had no history.
        // The live score is 320 because support has since driven it up. February's ledger says 40. If the
        // two are the same value the test cannot tell a correct read from a mutated one, so they differ.
        UUID fenceId = signal("Playground fence", "2026-02-06T09:00:00", "OPEN", 320.0);

        scoreEntry(fenceId, 40.0, LocalDateTime.parse("2026-02-06T10:00:00"));
        scoreEntry(fenceId, 320.0, LocalDateTime.parse("2026-04-02T10:00:00"));

        String report = getReport("2026-02");

        org.junit.jupiter.api.Assertions.assertTrue(
            unaddressedSection(report).contains("\"priorityScore\":40.0"),
            "February must show February's score, not the one the issue reached in April: "
                + unaddressedSection(report));
        org.junit.jupiter.api.Assertions.assertFalse(
            unaddressedSection(report).contains("\"priorityScore\":320.0"),
            "a later score leaked into a closed period: " + unaddressedSection(report));
    }

    private void scoreEntry(UUID signalId, double score, LocalDateTime at) {
        org.opencivic.signalos.domain.SignalScoreEntry entry =
            new org.opencivic.signalos.domain.SignalScoreEntry();
        entry.setId(UUID.randomUUID());
        entry.setSignalId(signalId);
        entry.setPriorityScore(score);
        entry.setUrgency(4);
        entry.setImpact(4);
        entry.setAffectedPeople(120);
        entry.setCommunityVotes(8);
        entry.setFormulaVersion("v1");
        entry.setCause(org.opencivic.signalos.domain.SignalScoreEntry.Cause.SUPPORT_VOTE);
        entry.setRecordedAt(at);
        scoreEntryRepository.save(entry);
    }

    @Test
    void rejectedSignalsShouldBeCountedSeparatelyAndStayReviewable() throws Exception {
        UUID rejected = signal("Duplicate of an existing report", "2026-02-06T09:00:00", "REJECTED", 12.0);
        addStatus(rejected, "OPEN", "REJECTED", LocalDateTime.parse("2026-02-09T09:00:00"));

        mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", "2026-02"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_REJECTED')].value").value(
                org.hamcrest.Matchers.contains(1)))
            .andExpect(jsonPath("$.metrics[?(@.key=='SIGNALS_RESOLVED')].value").value(
                org.hamcrest.Matchers.contains(0)))
            // A rejected item is not presented as a civic win.
            .andExpect(jsonPath("$.actioned", hasSize(0)));
    }

    @Test
    void malformedPeriodShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", "last-month"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nonMemberShouldNotReadACommunityReport() throws Exception {
        User outsider = new User("report_outsider", "encoded", "outsider@example.com", "ROLE_CITIZEN");
        outsider.setVerified(true);
        outsider.setEnabled(true);
        userRepository.save(outsider);

        // Non-membership raises UnauthorizedActionException, which this codebase maps to 401
        // rather than 403. Matching the existing convention rather than inventing a second one.
        mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_outsider").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", "2026-02"))
            .andExpect(status().isUnauthorized());
    }

    private String getReport(String period) throws Exception {
        return mockMvc.perform(get("/api/community/transparency-report")
                .with(user("report_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", period))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String stripGeneratedAt(String json) {
        return json.replaceAll("\"generatedAt\":\"[^\"]+\"", "\"generatedAt\":\"X\"");
    }

    private UUID signal(String title, String createdAt, String status, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(authorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the monthly report.");
        signal.setCategory("infrastructure");
        signal.setStatus(status);
        signal.setUrgency(3);
        signal.setImpact(3);
        signal.setAffectedPeople(50);
        signal.setCommunityVotes(5);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(3, 3, 50, 5));
        signal.setLocationLabel("Main corridor");
        signal.setCreatedAt(LocalDateTime.parse(createdAt));
        return signalRepository.save(signal).getId();
    }

    private void addStatus(UUID signalId, String from, String to, LocalDateTime at) {
        SignalStatusEntry entry = new SignalStatusEntry(
            signalId, from, to, "report_coord", "Recorded for the monthly report.");
        entry.setCreatedAt(at);
        statusEntryRepository.save(entry);
    }
}