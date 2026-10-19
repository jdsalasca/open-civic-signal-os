package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.InstitutionalTicketHandoffRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.InstitutionalTicketReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The export is bulk, so the handoff has to be too. A community sending fifty tickets should not
 * record fifty handoffs by hand.
 *
 * <p>Partial success is the expected outcome and is reported per signal rather than as one verdict.
 * A batch where three of fifty were already handed off should record forty-seven and say which three
 * were skipped, not fail entirely and leave the community to work out why.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:batchbridge;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstitutionalBatchHandoffIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private InstitutionalTicketHandoffRepository handoffRepository;

    private UUID communityId;
    private UUID coordinatorId;
    private UUID utilitiesSignal;
    private UUID roadsSignal;

    @BeforeEach
    void setUp() {
        User coordinator = new User("batch_coord", "encoded", "batch@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Batch handoff");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        utilitiesSignal = signal("Water main break", "utilities");
        roadsSignal = signal("Pothole on school route", "roads");
    }

    @Test
    void aBatchShouldRecordEveryMappedSignal() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchBody("""
                    {"utilities": "SRV-WATER-04", "roads": "SRV-ROADS-01"}
                    """, 30)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requested").value(2))
            .andExpect(jsonPath("$.recorded").value(2))
            .andExpect(jsonPath("$.skipped").value(0))
            .andExpect(jsonPath("$.outcomes", hasSize(2)))
            .andExpect(jsonPath("$.outcomes[0].outcome").value("RECORDED"))
            // The reference is the same one the export writes, so the city's file and our record agree.
            .andExpect(jsonPath("$.outcomes[0].detail").value(
                InstitutionalTicketReference.forSignal(utilitiesSignal)));

        org.junit.jupiter.api.Assertions.assertEquals(2, handoffRepository.count());
    }

    @Test
    void anUnmappedCategoryShouldBeSkippedAndNamed() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchBody("""
                    {"utilities": "SRV-WATER-04"}
                    """, 30)))
            .andExpect(status().isOk())
            // Partial success, not a whole-batch failure.
            .andExpect(jsonPath("$.recorded").value(1))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.outcomes[?(@.signalId=='%s')].outcome".formatted(roadsSignal))
                .value(org.hamcrest.Matchers.contains("SKIPPED")))
            // And it names the category, so the community knows what to map.
            .andExpect(jsonPath("$.outcomes[?(@.signalId=='%s')].detail".formatted(roadsSignal))
                .value(org.hamcrest.Matchers.contains(
                    containsString("No service code mapped for category 'roads'"))));
    }

    @Test
    void reRunningTheSameBatchShouldSkipRatherThanDuplicate() throws Exception {
        String body = batchBody("""
            {"utilities": "SRV-WATER-04", "roads": "SRV-ROADS-01"}
            """, 30);

        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recorded").value(2));

        // A retry must not create a second clock for the same complaint.
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recorded").value(0))
            .andExpect(jsonPath("$.skipped").value(2))
            .andExpect(jsonPath("$.outcomes[0].detail").value(containsString("Already handed off")));

        org.junit.jupiter.api.Assertions.assertEquals(2, handoffRepository.count());
    }

    @Test
    void aSignalFromAnotherCommunityShouldBeSkippedNotRecorded() throws Exception {
        Community other = new Community();
        other.setName("Elsewhere");
        other.setSlug("elsewhere");
        other.setDescription("Other");
        UUID otherId = communityRepository.save(other).getId();

        Signal foreign = new Signal();
        foreign.setId(UUID.randomUUID());
        foreign.setCommunityId(otherId);
        foreign.setTitle("Not ours");
        foreign.setDescription("Belongs elsewhere.");
        foreign.setCategory("utilities");
        foreign.setStatus("NEW");
        foreign.setPriorityScore(50.0);
        foreign.setScoreBreakdown(new ScoreBreakdown(3, 3, 40, 4));
        foreign.setCreatedAt(LocalDateTime.now());
        UUID foreignId = signalRepository.save(foreign).getId();

        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signalIds": ["%s", "%s"],
                      "categoryMap": {"utilities": "SRV-WATER-04"},
                      "slaTargetDays": 30
                    }
                    """.formatted(communityId, utilitiesSignal, foreignId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recorded").value(1))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.outcomes[?(@.signalId=='%s')].detail".formatted(foreignId))
                .value(org.hamcrest.Matchers.contains(
                    containsString("belongs to another community"))));
    }

    @Test
    void aBatchWithoutACategoryMapShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signalIds": ["%s"],
                      "slaTargetDays": 30
                    }
                    """.formatted(communityId, utilitiesSignal)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("category map is required")));
    }

    @Test
    void anOversizedBatchShouldBeRejectedRatherThanTruncated() throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            if (i > 0) {
                ids.append(",");
            }
            ids.append("\"").append(UUID.randomUUID()).append("\"");
        }

        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signalIds": [%s],
                      "categoryMap": {"utilities": "SRV-WATER-04"},
                      "slaTargetDays": 30
                    }
                    """.formatted(communityId, ids)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("At most 200 signals")));
    }

    @Test
    void anEmptyBatchShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signalIds": [],
                      "categoryMap": {"utilities": "SRV-WATER-04"}
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void theBatchShouldSayItStillCannotSeeTheInstitution() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchBody("""
                    {"utilities": "SRV-WATER-04", "roads": "SRV-ROADS-01"}
                    """, 30)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interpretation").value(
                containsString("cannot read the institution's system")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("not confirmations of receipt")));
    }

    @Test
    void aResidentWithoutTheScopeShouldNotBatchHandOff() throws Exception {
        User resident = new User("batch_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/institutional-handoffs/batch")
                .with(user("batch_resident").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchBody("""
                    {"utilities": "SRV-WATER-04"}
                    """, 30)))
            .andExpect(status().isForbidden());
    }

    private String batchBody(String categoryMap, int slaDays) {
        return """
            {
              "communityId": "%s",
              "signalIds": ["%s", "%s"],
              "categoryMap": %s,
              "slaTargetDays": %d
            }
            """.formatted(communityId, utilitiesSignal, roadsSignal, categoryMap, slaDays);
    }

    private UUID signal(String title, String category) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the batch handoff test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(200.0);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.now().minusDays(5));
        return signalRepository.save(signal).getId();
    }
}