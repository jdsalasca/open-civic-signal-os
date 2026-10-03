package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVoteMode;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
 * Residents ask to be contacted, so their own words carry contact details. These tests pin
 * that public open data never carries them out, and that the step is enforced rather than
 * merely available.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:anonit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicDataAnonymizationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private CommunityProposalRepository proposalRepository;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("anon_coord", "encoded", "anon@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Anonymization District");
        community.setSlug("anonymization-district");
        community.setDescription("PII at the publish boundary");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void checklistShouldBeCleanWhenNoRecordCarriesContactDetails() throws Exception {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle("Streetlight out on the main corridor");
        signal.setDescription("Three nights without light.");
        signal.setCategory("infrastructure");
        signal.setStatus("OPEN");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(82.4);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Main corridor");
        signal.setCreatedAt(LocalDateTime.now().minusDays(1));
        signalRepository.save(signal);

        mockMvc.perform(get("/api/community/exports/anonymization-check")
                .with(user("anon_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishable").value(true))
            .andExpect(jsonPath("$.blockingFindings", hasSize(0)))
            .andExpect(jsonPath("$.fields[?(@.exportType=='SIGNALS' && @.field=='title')].clean")
                .value(org.hamcrest.Matchers.contains(true)));
    }

    @Test
    void checklistShouldFlagFreeTextCarryingContactDetails() throws Exception {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle("Water outage on Calle 12");
        signal.setDescription("No water since yesterday.");
        signal.setCategory("utilities");
        signal.setStatus("OPEN");
        signal.setUrgency(5);
        signal.setImpact(5);
        signal.setAffectedPeople(300);
        signal.setCommunityVotes(20);
        signal.setPriorityScore(95.0);
        signal.setScoreBreakdown(new ScoreBreakdown(5, 5, 300, 20));
        signal.setLocationLabel("Calle 12, ask for maria.lopez@example.org");
        signal.setCreatedAt(LocalDateTime.now().minusDays(1));
        signalRepository.save(signal);

        mockMvc.perform(get("/api/community/exports/anonymization-check")
                .with(user("anon_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publishable").value(false))
            .andExpect(jsonPath("$.blockingFindings", hasSize(1)))
            .andExpect(jsonPath("$.blockingFindings[0]").value(
                org.hamcrest.Matchers.containsString("SIGNALS.locationLabel")))
            .andExpect(jsonPath("$.fields[?(@.exportType=='SIGNALS' && @.field=='title')].recordsWithFindings")
                .value(org.hamcrest.Matchers.contains(0)))
            .andExpect(jsonPath("$.fields[?(@.exportType=='SIGNALS' && @.field=='locationLabel')].categories[0]")
                .value(org.hamcrest.Matchers.hasItem("EMAIL")));
    }

    @Test
    void publishedExportShouldNotCarryContactDetailsOutOfTheRepository() throws Exception {
        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinatorId);
        proposal.setTitle("Repair the water main on Calle 12");
        proposal.setStatus("DRAFT");
        proposal.setProblemStatement("No supply since Tuesday.");
        proposal.setProposedSolution("Ask the utility at +34 600 11 22 33 to isolate the break.");
        proposal.setEstimatedCost("USD 9000");
        proposal.setBeneficiariesSummary("All residents on Calle 12");
        proposal.setSupportingLinks(java.util.List.of("https://example.com/water-plan"));
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setCreatedAt(LocalDateTime.now().minusHours(3));
        proposal.setUpdatedAt(LocalDateTime.now().minusHours(3));
        proposalRepository.save(proposal);

        String body = mockMvc.perform(get("/api/community/exports/PROPOSALS")
                .with(user("anon_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("format", "JSON"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("34600112233"),
            "export leaked a phone number: " + body);
        org.junit.jupiter.api.Assertions.assertTrue(
            body.contains("[phone redacted]"),
            "export should carry the visible redaction marker: " + body);
    }

    @Test
    void tokenCreationShouldBeBlockedWhileTheChecklistHasFindings() throws Exception {
        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinatorId);
        proposal.setTitle("Contact the utility");
        proposal.setStatus("DRAFT");
        proposal.setProblemStatement("No supply.");
        proposal.setProposedSolution("Email the utility at ops@utility.example.org");
        proposal.setEstimatedCost("USD 1200");
        proposal.setBeneficiariesSummary("Residents on Calle 12");
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setCreatedAt(LocalDateTime.now().minusHours(1));
        proposal.setUpdatedAt(LocalDateTime.now().minusHours(1));
        proposalRepository.save(proposal);

        mockMvc.perform(post("/api/community/exports/tokens")
                .with(user("anon_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "label": "publisher",
                      "scopes": ["EXPORT_PROPOSALS"],
                      "rateLimitPerHour": 10
                    }
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("anonymization-check")))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("acknowledgeResidualRisk")));
    }

    @Test
    void tokenCreationShouldProceedOnceResidualRiskIsAcknowledged() throws Exception {
        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinatorId);
        proposal.setTitle("Contact the utility");
        proposal.setStatus("DRAFT");
        proposal.setProblemStatement("No supply.");
        proposal.setProposedSolution("Email the utility at ops@utility.example.org");
        proposal.setEstimatedCost("USD 1200");
        proposal.setBeneficiariesSummary("Residents on Calle 12");
        proposal.setVoteMode(CommunityProposalVoteMode.YES_NO);
        proposal.setCreatedAt(LocalDateTime.now().minusHours(1));
        proposal.setUpdatedAt(LocalDateTime.now().minusHours(1));
        proposalRepository.save(proposal);

        mockMvc.perform(post("/api/community/exports/tokens")
                .with(user("anon_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "label": "publisher",
                      "scopes": ["EXPORT_PROPOSALS"],
                      "rateLimitPerHour": 10,
                      "acknowledgeResidualRisk": true
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token.scopes", hasSize(1)));
    }

    @Test
    void checklistShouldRequireTheExportPermissionScope() throws Exception {
        User member = new User("anon_member", "encoded", "member@example.com", "ROLE_CITIZEN");
        member.setVerified(true);
        member.setEnabled(true);
        UUID memberId = userRepository.save(member).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(memberId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(get("/api/community/exports/anonymization-check")
                .with(user("anon_member").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isForbidden());
    }
}