package org.opencivic.signalos;

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
 * The endpoint has to expose the arithmetic, not just a verdict. These tests assert on the
 * reason string and the individual baseline windows, because a badge a human cannot check is
 * the thing this project exists not to ship.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:surgeit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SurgeDetectionIT {

    private static final String WINDOW_END = "2026-03-08";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID communityId;
    private UUID analystId;

    @BeforeEach
    void setUp() {
        User analyst = new User("surge_analyst", "encoded", "surge@example.com", "ROLE_CITIZEN");
        analyst.setVerified(true);
        analyst.setEnabled(true);
        analystId = userRepository.save(analyst).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Trend detection");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(analystId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(analystId);
        membershipRepository.save(membership);
    }

    @Test
    void shouldFlagASurgeAndShowTheArithmetic() throws Exception {
        // Baseline: 2, 3, 2, 3 utilities reports in the four preceding weeks.
        baselineReports("utilities", LocalDateTime.parse("2026-02-03T10:00:00"), 2);
        baselineReports("utilities", LocalDateTime.parse("2026-02-12T10:00:00"), 3);
        baselineReports("utilities", LocalDateTime.parse("2026-02-18T10:00:00"), 2);
        baselineReports("utilities", LocalDateTime.parse("2026-02-26T10:00:00"), 3);
        // Current window: 14, against a median baseline of 2 or 3.
        baselineReports("utilities", LocalDateTime.parse("2026-03-03T10:00:00"), 14);

        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v1"))
            .andExpect(jsonPath("$.surges", hasSize(1)))
            .andExpect(jsonPath("$.surges[0].category").value("utilities"))
            .andExpect(jsonPath("$.surges[0].verdict").value("SURGE"))
            .andExpect(jsonPath("$.surges[0].currentCount").value(14))
            .andExpect(jsonPath("$.surges[0].baselineMedian").value(3))
            // The reason has to show the comparison and the threshold it crossed.
            .andExpect(jsonPath("$.surges[0].reason").value(
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("14 reports"),
                    org.hamcrest.Matchers.containsString("baseline median of 3"),
                    org.hamcrest.Matchers.containsString("threshold"))))
            // Individual windows travel with the verdict so a human can check the median.
            .andExpect(jsonPath("$.surges[0].baselineCounts", hasSize(4)))
            .andExpect(jsonPath("$.thresholds.minCurrentCount").value(5))
            .andExpect(jsonPath("$.thresholds.surgeMultiplier").value(2.0));
    }

    @Test
    void shouldSuppressAQuietZoneTriplingAndCountIt() throws Exception {
        baselineReports("infrastructure", LocalDateTime.parse("2026-02-10T10:00:00"), 1);
        baselineReports("infrastructure", LocalDateTime.parse("2026-02-18T10:00:00"), 1);
        baselineReports("infrastructure", LocalDateTime.parse("2026-02-25T10:00:00"), 1);
        baselineReports("infrastructure", LocalDateTime.parse("2026-03-04T10:00:00"), 3);

        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.surges", hasSize(0)))
            .andExpect(jsonPath("$.zonesSuppressedByMinVolume").value(1))
            .andExpect(jsonPath("$.zonesEvaluated[0].verdict").value("SUPPRESSED_LOW_VOLUME"))
            .andExpect(jsonPath("$.zonesEvaluated[0].reason").value(
                org.hamcrest.Matchers.containsString("below the minimum of 5")));
    }

    @Test
    void shouldExcludeTheCurrentWindowFromTheBaseline() throws Exception {
        // Everything lands in the current window; if it also counted as baseline the median
        // would jump and the surge would vanish.
        baselineReports("utilities", LocalDateTime.parse("2026-03-04T10:00:00"), 12);

        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.surges[0].currentCount").value(12))
            .andExpect(jsonPath("$.surges[0].baselineMedian").value(0))
            .andExpect(jsonPath("$.surges[0].verdict").value("SURGE"))
            .andExpect(jsonPath("$.surges[0].reason").value(
                org.hamcrest.Matchers.containsString("no-baseline")));
    }

    @Test
    void thresholdsShouldBeTunablePerCommunityWithoutRedeploying() throws Exception {
        // Three baseline windows with 4 reports each and one empty window gives a median of 4:
        // at least half the baseline has to look normal before a ratio means anything.
        baselineReports("utilities", LocalDateTime.parse("2026-02-10T10:00:00"), 4);
        baselineReports("utilities", LocalDateTime.parse("2026-02-18T10:00:00"), 4);
        baselineReports("utilities", LocalDateTime.parse("2026-02-25T10:00:00"), 4);
        baselineReports("utilities", LocalDateTime.parse("2026-03-04T10:00:00"), 6);

        // 6 against 4 is 1.5x: below the default 2.0x threshold.
        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.surges", hasSize(0)))
            .andExpect(jsonPath("$.zonesEvaluated[0].baselineMedian").value(4));

        // The same data is a surge once the community says 1.2x is enough for its volume.
        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END)
                .queryParam("surgeMultiplier", "1.2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.surges", hasSize(1)))
            .andExpect(jsonPath("$.thresholds.surgeMultiplier").value(1.2));
    }

    @Test
    void invalidThresholdsShouldBeRejectedRatherThanSilentlyCoerced() throws Exception {
        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END)
                .queryParam("surgeMultiplier", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldOnlyReportOnTheCallersOwnCommunities() throws Exception {
        Community other = new Community();
        other.setName("Elsewhere District");
        other.setSlug("elsewhere-district");
        other.setDescription("Not visible");
        communityRepository.save(other);

        baselineReports("utilities", LocalDateTime.parse("2026-03-04T10:00:00"), 12);

        // No global access and the caller is a member here, so only this community appears.
        mockMvc.perform(get("/api/signals/surges")
                .with(user("surge_analyst").roles("CITIZEN"))
                .queryParam("windowEnd", WINDOW_END))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zonesConsidered").value(1))
            .andExpect(jsonPath("$.surges[0].communityId").value(communityId.toString()));
    }

    @Test
    void formulaEndpointShouldBeReadableWithoutFetchingTheReport() throws Exception {
        mockMvc.perform(get("/api/signals/surges/formula")
                .with(user("surge_analyst").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v1"))
            .andExpect(jsonPath("$.thresholds.baselineWindows").value(4))
            .andExpect(jsonPath("$.windowDays").value(7))
            .andExpect(jsonPath("$.baselinePeriods", hasSize(4)));
    }

    private void baselineReports(String category, LocalDateTime firstAt, int count) {
        for (int i = 0; i < count; i++) {
            Signal signal = new Signal();
            signal.setId(UUID.randomUUID());
            signal.setCommunityId(communityId);
            signal.setAuthorId(analystId);
            signal.setTitle("Water report " + category + " " + i);
            signal.setDescription("Reported during the seeded baseline window.");
            signal.setCategory(category);
            signal.setStatus("OPEN");
            signal.setUrgency(3);
            signal.setImpact(3);
            signal.setAffectedPeople(50);
            signal.setCommunityVotes(4);
            signal.setPriorityScore(60.0);
            signal.setScoreBreakdown(new ScoreBreakdown(3, 3, 50, 4));
            signal.setLocationLabel("Riverside");
            signal.setCreatedAt(firstAt.plusHours(i));
            signalRepository.save(signal);
        }
    }
}