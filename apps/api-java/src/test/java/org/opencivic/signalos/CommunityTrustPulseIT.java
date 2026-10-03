package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityTrustPulseResponseRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The pulse measures what residents think, and the rules that keep it honest are the ones under test.
 *
 * <p>An average over three answers is noise, and publishing it as a community verdict invites a
 * decision on that noise. A pulse that counts the loudest respondent twice is not a measure of the
 * community. And a resident's free text is not a public dataset row.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:trustpulse;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityTrustPulseIT {

    private static final String PERIOD = "2026-03";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityTrustPulseResponseRepository pulseRepository;

    private UUID communityId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        User member = new User("pulse_member", "encoded", "pulse@example.com", "ROLE_CITIZEN");
        member.setVerified(true);
        member.setEnabled(true);
        memberId = userRepository.save(member).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Trust pulse");
        communityId = communityRepository.save(community).getId();

        addMember("pulse_member", CommunityRole.MEMBER);
    }

    @Test
    void aResidentShouldBeAbleToAnswerThePulse() throws Exception {
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(4, 3, 5, "The new map view is genuinely useful.")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.periodKey").value(PERIOD))
            .andExpect(jsonPath("$.replacedPrevious").value(false))
            .andExpect(jsonPath("$.message").value(containsString("Recorded")));
    }

    @Test
    void answeringAgainShouldReplaceRatherThanStack() throws Exception {
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(2, 2, 2, null)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(5, 5, 5, "Changed my mind after the fix.")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replacedPrevious").value(true))
            .andExpect(jsonPath("$.message").value(containsString("replaced")));

        // One person, one answer. A pulse that counts the loudest respondent twice is not a measure
        // of the community.
        org.junit.jupiter.api.Assertions.assertEquals(1, pulseRepository.count());
    }

    @Test
    void noAverageShouldBeReportedBelowTheMinimumSample() throws Exception {
        submitAs("pulse_member", 5, 5, 5);
        addMember("pulse_second", CommunityRole.MEMBER);
        submitAs("pulse_second", 5, 5, 5);

        mockMvc.perform(get("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", PERIOD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responses").value(2))
            .andExpect(jsonPath("$.enoughForAverage").value(false))
            .andExpect(jsonPath("$.dimensions", hasSize(3)))
            // No mean at all, rather than a mean with a caveat nobody reads.
            .andExpect(jsonPath("$.dimensions[0].average").doesNotExist())
            .andExpect(jsonPath("$.dimensions[0].note").value(containsString("Not enough responses")))
            .andExpect(jsonPath("$.interpretation").value(containsString("no average is reported at all")));
    }

    @Test
    void anAverageShouldBeReportedOnceTheSampleIsEnough() throws Exception {
        submitAs("pulse_member", 4, 2, 5);
        for (int i = 0; i < 4; i++) {
            String username = "pulse_extra_" + i;
            addMember(username, CommunityRole.MEMBER);
            submitAs(username, 4, 2, 5);
        }

        mockMvc.perform(get("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", PERIOD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responses").value(5))
            .andExpect(jsonPath("$.enoughForAverage").value(true))
            .andExpect(jsonPath("$.dimensions[?(@.key=='trust')].average").value(
                org.hamcrest.Matchers.contains(4.0)))
            .andExpect(jsonPath("$.dimensions[?(@.key=='responsiveness')].average").value(
                org.hamcrest.Matchers.contains(2.0)))
            .andExpect(jsonPath("$.dimensions[?(@.key=='transparency')].average").value(
                org.hamcrest.Matchers.contains(5.0)));
    }

    @Test
    void theAggregateShouldSayItMeasuresPerceptionNotPerformance() throws Exception {
        submitAs("pulse_member", 3, 3, 3);

        mockMvc.perform(get("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", PERIOD))
            .andExpect(status().isOk())
            // A number labelled "trust" invites a reader to treat it as a performance measure.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("not a measure of what the platform did")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("self-selected")))
            // And it must say what happens to free text.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("Comments are never published verbatim")));
    }

    @Test
    void aScoreOutsideTheScaleShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(9, 3, 3, null)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("between 1 and 5")));
    }

    @Test
    void aMalformedPeriodShouldBeRejectedRatherThanGuessedAt() throws Exception {
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "period": "last month",
                      "trustScore": 3, "responsivenessScore": 3, "transparencyScore": 3
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anOutsiderShouldNotAnswerOrReadThePulse() throws Exception {
        User outsider = new User("pulse_outsider", "encoded", "out@example.com", "ROLE_CITIZEN");
        outsider.setVerified(true);
        outsider.setEnabled(true);
        userRepository.save(outsider);

        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_outsider").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(5, 5, 5, null)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/community/trust-pulse")
                .with(user("pulse_outsider").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("period", PERIOD))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void historyShouldListMonthsNewestFirst() throws Exception {
        submitAs("pulse_member", 3, 3, 3);
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user("pulse_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "period": "2026-02",
                      "trustScore": 2, "responsivenessScore": 2, "transparencyScore": 2
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/trust-pulse/history")
                .with(user("pulse_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].periodKey").value("2026-03"))
            .andExpect(jsonPath("$[1].periodKey").value("2026-02"));
    }

    @Test
    void theMinimumSampleShouldBeReadableSoAClientCanExplainIt() throws Exception {
        mockMvc.perform(get("/api/community/trust-pulse/minimum-sample")
                .with(user("pulse_member").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(5));
    }

    private void submitAs(String username, int trust, int responsiveness, int transparency) throws Exception {
        mockMvc.perform(post("/api/community/trust-pulse")
                .with(user(username).roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(submission(trust, responsiveness, transparency, null)))
            .andExpect(status().isOk());
    }

    private String submission(int trust, int responsiveness, int transparency, String comment) {
        return """
            {
              "communityId": "%s",
              "period": "%s",
              "trustScore": %d,
              "responsivenessScore": %d,
              "transparencyScore": %d,
              "comment": %s
            }
            """.formatted(
                communityId, PERIOD, trust, responsiveness, transparency,
                comment == null ? "null" : "\"" + comment + "\"");
    }

    private void addMember(String username, CommunityRole role) {
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User created = new User(username, "encoded", username + "@example.com", "ROLE_CITIZEN");
            created.setVerified(true);
            created.setEnabled(true);
            return userRepository.save(created);
        });
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(user.getId());
        membership.setRole(role);
        membership.setCreatedBy(memberId);
        membershipRepository.save(membership);
    }
}