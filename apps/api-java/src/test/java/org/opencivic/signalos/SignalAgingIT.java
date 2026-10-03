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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:agingit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalAgingIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;

    private UUID communityId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        User citizen = new User("aging_citizen", "encoded", "aging@example.com", "ROLE_CITIZEN");
        citizen.setVerified(true);
        citizen.setEnabled(true);
        authorId = userRepository.save(citizen).getId();

        Community community = new Community();
        community.setName("Aging District");
        community.setSlug("aging-district");
        community.setDescription("SLA tracking");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(authorId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(authorId);
        membershipRepository.save(membership);
    }

    private Signal saveSignal(String title, String status, int ageDays) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(authorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for SLA tracking");
        signal.setCategory("infrastructure");
        signal.setStatus(status);
        signal.setUrgency(3);
        signal.setImpact(3);
        signal.setAffectedPeople(50);
        signal.setPriorityScore(120);
        signal.setCreatedAt(LocalDateTime.now().minusDays(ageDays));
        return signalRepository.save(signal);
    }

    @Test
    void unresolvedCasesShouldBeBucketedAndRankedByRisk() throws Exception {
        saveSignal("Fresh case", "NEW", 2);
        saveSignal("Aging case", "IN_PROGRESS", 10);
        saveSignal("Stale case", "IN_PROGRESS", 26);
        saveSignal("Breached case", "IN_PROGRESS", 45);
        saveSignal("Done case", "RESOLVED", 45);

        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_citizen").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("slaTargetDays", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaTargetDays").value(30))
            .andExpect(jsonPath("$.unresolvedCount").value(4))
            .andExpect(jsonPath("$.atRiskCount").value(1))
            .andExpect(jsonPath("$.breachedCount").value(1))
            .andExpect(jsonPath("$.ageBuckets", hasSize(4)))
            .andExpect(jsonPath("$.ageBuckets[?(@.bucket=='FRESH_0_7')].count").value(hasSize(1)))
            .andExpect(jsonPath("$.ageBuckets[?(@.bucket=='OVERDUE')].count").value(hasSize(1)))
            .andExpect(jsonPath("$.atRiskSignals", hasSize(2)))
            .andExpect(jsonPath("$.atRiskSignals[0].title").value("Breached case"))
            .andExpect(jsonPath("$.atRiskSignals[0].slaRisk").value("BREACHED"))
            .andExpect(jsonPath("$.atRiskSignals[0].daysOverTarget").value(15));
    }

    @Test
    void medianAgeShouldBeReportedForUnresolvedCases() throws Exception {
        saveSignal("A", "NEW", 1);
        saveSignal("B", "NEW", 3);
        saveSignal("C", "NEW", 5);

        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_citizen").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.medianAgeDays").value(3));
    }

    @Test
    void defaultTargetShouldApplyWhenOmitted() throws Exception {
        saveSignal("A", "NEW", 1);

        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_citizen").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slaTargetDays").value(30))
            .andExpect(jsonPath("$.unresolvedCount").value(1));
    }

    @Test
    void trendShouldReportCreatedAndResolvedPerDay() throws Exception {
        Signal resolved = saveSignal("Closed today", "RESOLVED", 1);

        SignalStatusEntry entry = new SignalStatusEntry(
            resolved.getId(), "IN_PROGRESS", "RESOLVED", "STATUS_CHANGED", "liaison",
            "Work finished", null
        );
        entry.setCreatedAt(LocalDateTime.now().minusHours(2));
        statusEntryRepository.save(entry);

        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_citizen").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trend", hasSize(30)))
            .andExpect(jsonPath("$.trend[?(@.date=='"
                + LocalDateTime.now().toLocalDate()
                + "')].created").value(hasSize(1)))
            .andExpect(jsonPath("$.trend[?(@.date=='"
                + LocalDateTime.now().toLocalDate()
                + "')].resolved").value(hasSize(1)));
    }

    @Test
    void nonMemberShouldNotReadCommunityAging() throws Exception {
        User outsider = new User("aging_outsider", "encoded", "out@example.com", "ROLE_CITIZEN");
        outsider.setVerified(true);
        outsider.setEnabled(true);
        userRepository.save(outsider);

        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_outsider").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void riskBoundariesShouldFollowThePublishedRatio() {
        org.junit.jupiter.api.Assertions.assertEquals(
            "ON_TRACK", org.opencivic.signalos.service.SignalAgingService.resolveRisk(23, 30)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "AT_RISK", org.opencivic.signalos.service.SignalAgingService.resolveRisk(24, 30)
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "BREACHED", org.opencivic.signalos.service.SignalAgingService.resolveRisk(31, 30)
        );
    }

    @Test
    void mediaTypeShouldStayJson() throws Exception {
        mockMvc.perform(get("/api/signals/aging")
                .with(user("aging_citizen").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}