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
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.InstitutionalTicketHandoffRepository;
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

/**
 * The platform cannot read a city helpdesk, and the whole design follows from that.
 *
 * <p>Every status is either declared by a person or derived from our own signal lifecycle, and each
 * handoff says which. A "synchronised" badge that meant "we guessed" would be worse than no badge,
 * because a community would act on it.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:bridge;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstitutionalActionBridgeIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository statusEntryRepository;
    @Autowired private InstitutionalTicketHandoffRepository handoffRepository;

    private UUID communityId;
    private UUID coordinatorId;
    private UUID signalId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("bridge_coord", "encoded", "bridge@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Institutional bridge");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        signalId = signal("Water main break", "utilities");
    }

    @Test
    void recordingAHandoffShouldStartTheClock() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "SRV-WATER-04", 30)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketRef").value("OCS-ABC123"))
            .andExpect(jsonPath("$.externalCategoryCode").value("SRV-WATER-04"))
            .andExpect(jsonPath("$.slaTargetDays").value(30))
            .andExpect(jsonPath("$.slaState").value("WITHIN_TARGET"))
            // Nothing has been declared yet, and the record says so rather than assuming.
            .andExpect(jsonPath("$.statusSource").value("NO_INSTITUTIONAL_STATUS"))
            .andExpect(jsonPath("$.acknowledgedAt").doesNotExist())
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    @Test
    void aSecondHandoffForTheSameSignalShouldBeRefused() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "SRV-WATER-04", 30)))
            .andExpect(status().isOk());

        // Two clocks for one complaint means nobody can say which one was late.
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "SRV-WATER-04", 30)))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("second clock")));

        org.junit.jupiter.api.Assertions.assertEquals(1, handoffRepository.count());
    }

    @Test
    void aDeclaredResolutionShouldBeAttributedToTheCommunity() throws Exception {
        String created = recordHandoff("OCS-ABC123", 30);
        String handoffId = created.replaceAll("(?s).*\"handoffId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/community/institutional-handoffs/{id}/outcome", handoffId)
                .with(user("bridge_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"resolved": true, "note": "City confirmed the repair on the 14th."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
            // Resolving implies acknowledging; leaving it empty would say the city never picked it up.
            .andExpect(jsonPath("$.acknowledgedAt").isNotEmpty())
            .andExpect(jsonPath("$.statusSource").value("DECLARED_BY_COMMUNITY"))
            .andExpect(jsonPath("$.slaState").value("CLOSED_ON_TIME"));
    }

    @Test
    void resolvingOurOwnSignalShouldDeriveTheHandoffOutcome() throws Exception {
        recordHandoff("OCS-ABC123", 30);

        // The community resolves the signal. The resident's problem is dealt with, which is what the
        // handoff was for, so the handoff is treated as resolved.
        Signal signal = signalRepository.findById(signalId).orElseThrow();
        signal.setStatus("RESOLVED");
        signalRepository.save(signal);
        SignalStatusEntry entry = new SignalStatusEntry(
            signalId, "NEW", "RESOLVED", "bridge_coord", "Fixed.");
        entry.setCreatedAt(LocalDateTime.now());
        statusEntryRepository.save(entry);

        mockMvc.perform(get("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.handoffs[0].resolvedAt").isNotEmpty())
            // And it says the platform derived that, rather than being told by the city.
            .andExpect(jsonPath("$.handoffs[0].statusSource").value("DERIVED_FROM_SIGNAL_LIFECYCLE"));
    }

    @Test
    void anOverdueHandoffShouldBeJudgedAgainstTheCommunitysOwnTarget() throws Exception {
        // A handoff recorded 40 days ago with a 30 day target.
        var handoff = new org.opencivic.signalos.domain.InstitutionalTicketHandoff();
        handoff.setId(UUID.randomUUID());
        handoff.setCommunityId(communityId);
        handoff.setSignalId(signalId);
        handoff.setTicketRef("OCS-OLD");
        handoff.setExternalCategoryCode("SRV-WATER-04");
        handoff.setHandedOffBy(coordinatorId);
        handoff.setHandedOffAt(LocalDateTime.now().minusDays(40));
        handoff.setSlaTargetDays(30);
        handoffRepository.save(handoff);

        mockMvc.perform(get("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overdue").value(1))
            .andExpect(jsonPath("$.handoffs[0].slaState").value("OVERDUE"))
            .andExpect(jsonPath("$.handoffs[0].daysOverTarget").value(10));
    }

    @Test
    void theSummaryShouldSayItCannotSeeTheInstitution() throws Exception {
        recordHandoff("OCS-ABC123", 30);

        mockMvc.perform(get("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openHandoffs").value(1))
            // A community reading "overdue" needs to know the platform did not observe that.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does NOT read the institution's system")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("not that the city has failed")));
    }

    @Test
    void aHandoffWithoutATicketReferenceShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("   ", "SRV-WATER-04", 30)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("ticket reference is required")));
    }

    @Test
    void aHandoffWithoutAServiceCodeShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "  ", 30)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("external category code is required")));
    }

    @Test
    void aSignalFromAnotherCommunityShouldNotBeHandedOff() throws Exception {
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

        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "signalId": "%s",
                      "ticketRef": "OCS-X",
                      "externalCategoryCode": "SRV-1",
                      "slaTargetDays": 30
                    }
                    """.formatted(communityId, foreignId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("wrong place")));
    }

    @Test
    void anAbsurdSlaTargetShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "SRV-WATER-04", 5000)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void aResidentWithoutTheScopeShouldNotHandOffOrRead() throws Exception {
        User resident = new User("bridge_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_resident").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody("OCS-ABC123", "SRV-WATER-04", 30)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/community/institutional-handoffs")
                .with(user("bridge_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isForbidden());
    }

    private String recordHandoff(String ticketRef, int slaDays) throws Exception {
        return mockMvc.perform(post("/api/community/institutional-handoffs")
                .with(user("bridge_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(handoffBody(ticketRef, "SRV-WATER-04", slaDays)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String handoffBody(String ticketRef, String categoryCode, int slaDays) {
        return """
            {
              "communityId": "%s",
              "signalId": "%s",
              "ticketRef": "%s",
              "externalCategoryCode": "%s",
              "slaTargetDays": %d
            }
            """.formatted(communityId, signalId, ticketRef, categoryCode, slaDays);
    }

    private UUID signal(String title, String category) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the institutional bridge test.");
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