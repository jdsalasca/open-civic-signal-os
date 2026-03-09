package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalDeliberationEntry;
import org.opencivic.signalos.domain.CommunityProposalDeliberationType;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.CommunityThread;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalDeliberationEntryRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityThreadMessageRepository;
import org.opencivic.signalos.repository.CommunityThreadRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:moderationit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityModerationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private CommunityThreadRepository threadRepository;
    @Autowired private CommunityThreadMessageRepository threadMessageRepository;
    @Autowired private CommunityProposalRepository proposalRepository;
    @Autowired private CommunityProposalDeliberationEntryRepository proposalEntryRepository;

    private UUID communityId;
    private UUID threadId;
    private UUID threadMessageId;
    private UUID proposalEntryId;

    @BeforeEach
    void setUp() {
        User moderator = createUser("moderation_mod", true);
        User reporter = createUser("moderation_reporter", true);
        User accused = createUser("moderation_accused", true);

        Community community = new Community();
        community.setName("Moderation District");
        community.setSlug("moderation-district");
        community.setDescription("Moderation queue test scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        membershipRepository.save(createMembership(communityId, moderator.getId(), moderator.getId(), CommunityRole.MODERATOR));
        membershipRepository.save(createMembership(communityId, reporter.getId(), moderator.getId(), CommunityRole.MEMBER));
        membershipRepository.save(createMembership(communityId, accused.getId(), moderator.getId(), CommunityRole.MEMBER));

        CommunityThread thread = new CommunityThread();
        thread.setSourceCommunityId(communityId);
        thread.setTargetCommunityId(communityId);
        thread.setTitle("Neighborhood coordination thread");
        thread.setCreatedBy(reporter.getId());
        thread.setCreatedAt(LocalDateTime.now().minusDays(1));
        thread.setUpdatedAt(LocalDateTime.now().minusHours(2));
        thread = threadRepository.save(thread);
        threadId = thread.getId();

        CommunityThreadMessage message = new CommunityThreadMessage();
        message.setThreadId(threadId);
        message.setAuthorId(accused.getId());
        message.setSourceCommunityId(communityId);
        message.setContent("You should stop posting here because nobody wants your updates.");
        message.setCreatedAt(LocalDateTime.now().minusHours(1));
        message = threadMessageRepository.save(message);
        threadMessageId = message.getId();

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(reporter.getId());
        proposal.setTitle("Safer plaza access");
        proposal.setProblemStatement("People need a calmer and safer path into the plaza.");
        proposal.setProposedSolution("Rework the plaza crossing with signage and volunteer marshals.");
        proposal.setEstimatedCost("COP 8M");
        proposal.setBeneficiariesSummary("Families, older adults, and students.");
        proposal.setCreatedAt(LocalDateTime.now().minusDays(2));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);

        CommunityProposalDeliberationEntry proposalEntry = new CommunityProposalDeliberationEntry();
        proposalEntry.setProposalId(proposal.getId());
        proposalEntry.setAuthorId(accused.getId());
        proposalEntry.setEntryType(CommunityProposalDeliberationType.EVIDENCE);
        proposalEntry.setContent("This claim cites a document that does not exist.");
        proposalEntry.setCreatedAt(LocalDateTime.now().minusHours(3));
        proposalEntry.setUpdatedAt(LocalDateTime.now().minusHours(3));
        proposalEntry = proposalEntryRepository.save(proposalEntry);
        proposalEntryId = proposalEntry.getId();
    }

    @Test
    @WithMockUser(username = "moderation_reporter", roles = {"CITIZEN"})
    void memberCanReportCommunityContentAndModeratorCanEnforceSanction() throws Exception {
        String reportId = objectMapper.readTree(
            mockMvc.perform(post("/api/community/moderation/reports")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "communityId", communityId,
                        "targetType", "THREAD_MESSAGE",
                        "targetId", threadMessageId,
                        "reasonCode", "HARASSMENT",
                        "details", "The message targets a neighbor personally and discourages participation."
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reasonCode").value("HARASSMENT"))
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("id").asText();

        mockMvc.perform(get("/api/community/moderation/queue")
                .with(user("moderation_mod").roles("CITIZEN"))
                .param("communityId", communityId.toString())
                .param("status", "OPEN")
                .param("targetType", "THREAD_MESSAGE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openReports").value(1))
            .andExpect(jsonPath("$.reports[0].targetId").value(threadMessageId.toString()))
            .andExpect(jsonPath("$.reports[0].falsePositiveReviewRecommended").value(false));

        mockMvc.perform(patch("/api/community/moderation/reports/{reportId}", reportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "action", "ENFORCE",
                    "hideContent", true,
                    "sanctionType", "SUSPEND_7_DAYS",
                    "resolutionReason", "Personal attacks are not allowed in this community workspace."
                ))))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderation_mod", roles = {"CITIZEN"})
    void moderatorCanEnforceAndDismissReportsWithAuditableOutcomes() throws Exception {
        MvcResult threadReport = mockMvc.perform(post("/api/community/moderation/reports")
                .with(user("moderation_reporter").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "communityId", communityId,
                    "targetType", "THREAD_MESSAGE",
                    "targetId", threadMessageId,
                    "reasonCode", "HARASSMENT",
                    "details", "The message targets a neighbor personally and discourages participation."
                ))))
            .andExpect(status().isOk())
            .andReturn();

        String threadReportId = objectMapper.readTree(threadReport.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/community/moderation/reports/{reportId}", threadReportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "action", "ENFORCE",
                    "hideContent", true,
                    "sanctionType", "SUSPEND_7_DAYS",
                    "resolutionReason", "Personal attacks are not allowed in this community workspace."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIONED"))
            .andExpect(jsonPath("$.contentHidden").value(true))
            .andExpect(jsonPath("$.sanction.sanctionType").value("SUSPEND_7_DAYS"))
            .andExpect(jsonPath("$.actionHistory[?(@.actionType=='SANCTION_ISSUED')]").isNotEmpty());

        mockMvc.perform(post("/api/community/threads/{threadId}/messages", threadId)
                .with(user("moderation_accused").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "sourceCommunityId", communityId,
                    "content", "Trying to post while suspended."
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("COMMUNITY_SANCTION_ACTIVE"));

        MvcResult proposalReport = mockMvc.perform(post("/api/community/moderation/reports")
                .with(user("moderation_reporter").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "communityId", communityId,
                    "targetType", "PROPOSAL_DELIBERATION",
                    "targetId", proposalEntryId,
                    "reasonCode", "MISINFORMATION",
                    "details", "The evidence claim appears unverifiable and should be reviewed before removal."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.falsePositiveReviewRecommended").value(true))
            .andReturn();

        String proposalReportId = objectMapper.readTree(proposalReport.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/community/moderation/reports/{reportId}", proposalReportId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "action", "DISMISS",
                    "hideContent", false,
                    "resolutionReason", "Evidence was weak, but the report did not justify enforcement."
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISMISSED"))
            .andExpect(jsonPath("$.sanction").doesNotExist());

        mockMvc.perform(get("/api/community/moderation/queue")
                .param("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actionedReports").value(1))
            .andExpect(jsonPath("$.dismissedReports").value(1))
            .andExpect(jsonPath("$.activeSanctions").value(1));
    }

    private User createUser(String username, boolean verified) {
        User user = new User(username, "{noop}pw", username + "@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(verified);
        return userRepository.save(user);
    }

    private CommunityMembership createMembership(UUID communityId, UUID userId, UUID createdBy, CommunityRole role) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(createdBy);
        return membership;
    }
}
