package org.opencivic.signalos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.CommunityThread;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityThreadMessageRepository;
import org.opencivic.signalos.repository.CommunityThreadRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityRBAC_IT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private CommunityThreadRepository threadRepository;

    @Autowired
    private CommunityThreadMessageRepository messageRepository;

    private UUID communityId;
    private UUID memberUserId;
    private UUID coordinatorUserId;
    private UUID liaisonUserId;
    private UUID targetUserId;
    private UUID threadId;
    private UUID recentThreadId;
    private UUID staleThreadId;
    private UUID messageId;
    private UUID recentThreadMessageId;

    @BeforeEach
    void setUp() {
        User member = createUser("member_user");
        User coordinator = createUser("coord_user");
        User liaison = createUser("liaison_user");
        User target = createUser("target_user");

        memberUserId = member.getId();
        coordinatorUserId = coordinator.getId();
        liaisonUserId = liaison.getId();
        targetUserId = target.getId();

        Community community = new Community();
        community.setName("Central District");
        community.setSlug("central-district");
        community.setDescription("Test community");
        community = communityRepository.save(community);
        communityId = community.getId();

        addMembership(memberUserId, CommunityRole.MEMBER, memberUserId);
        addMembership(coordinatorUserId, CommunityRole.COORDINATOR, coordinatorUserId);
        addMembership(liaisonUserId, CommunityRole.PUBLIC_SERVANT_LIAISON, coordinatorUserId);
        addMembership(targetUserId, CommunityRole.MEMBER, coordinatorUserId);

        CommunityThread thread = new CommunityThread();
        thread.setSourceCommunityId(communityId);
        thread.setTargetCommunityId(communityId);
        thread.setTitle("Cross-neighborhood flood response");
        thread.setCreatedBy(coordinatorUserId);
        thread.setCreatedAt(LocalDateTime.now());
        thread.setUpdatedAt(LocalDateTime.now());
        thread = threadRepository.save(thread);
        threadId = thread.getId();

        CommunityThread recentThread = new CommunityThread();
        recentThread.setSourceCommunityId(communityId);
        recentThread.setTargetCommunityId(communityId);
        recentThread.setTitle("Recent active thread for paging order");
        recentThread.setCreatedBy(coordinatorUserId);
        recentThread.setCreatedAt(LocalDateTime.now().minusDays(1));
        recentThread.setUpdatedAt(LocalDateTime.now().minusHours(4));
        recentThread = threadRepository.save(recentThread);
        recentThreadId = recentThread.getId();

        CommunityThread staleThread = new CommunityThread();
        staleThread.setSourceCommunityId(communityId);
        staleThread.setTargetCommunityId(communityId);
        staleThread.setTitle("Stale thread for pagination filter");
        staleThread.setCreatedBy(coordinatorUserId);
        staleThread.setCreatedAt(LocalDateTime.now().minusDays(14));
        staleThread.setUpdatedAt(LocalDateTime.now().minusDays(10));
        staleThread = threadRepository.save(staleThread);
        staleThreadId = staleThread.getId();

        CommunityThreadMessage message = new CommunityThreadMessage();
        message.setThreadId(threadId);
        message.setAuthorId(memberUserId);
        message.setSourceCommunityId(communityId);
        message.setContent("Initial message");
        message = messageRepository.save(message);
        messageId = message.getId();

        CommunityThreadMessage recentMessage = new CommunityThreadMessage();
        recentMessage.setThreadId(recentThreadId);
        recentMessage.setAuthorId(memberUserId);
        recentMessage.setSourceCommunityId(communityId);
        recentMessage.setContent("Recent message with strong engagement");
        recentMessage.setReactions(Map.of("🔥", 5, "👍", 3));
        recentMessage = messageRepository.save(recentMessage);
        recentThreadMessageId = recentMessage.getId();

        CommunityThreadMessage recentReply = new CommunityThreadMessage();
        recentReply.setThreadId(recentThreadId);
        recentReply.setAuthorId(targetUserId);
        recentReply.setSourceCommunityId(communityId);
        recentReply.setParentMessageId(recentThreadMessageId);
        recentReply.setContent("Reply keeps this discussion active");
        recentReply = messageRepository.save(recentReply);
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCannotModerateThreadMessage() throws Exception {
        String path = "/api/community/threads/" + threadId + "/messages/" + messageId + "/moderate";
        mockMvc.perform(
                patch(path)
                    .contentType("application/json")
                    .content("{\"hidden\":true,\"reason\":\"Abusive content\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coord_user", roles = {"CITIZEN"})
    void coordinatorCanUpdateMembershipRole() throws Exception {
        String path = "/api/communities/" + communityId + "/memberships/" + targetUserId + "/role";
        mockMvc.perform(
                patch(path)
                    .contentType("application/json")
                    .content("{\"role\":\"MODERATOR\"}")
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCannotCreateBlogPost() throws Exception {
        mockMvc.perform(
                post("/api/community/blog")
                    .contentType("application/json")
                    .content(
                        "{\"communityId\":\"" + communityId + "\",\"title\":\"Update\",\"content\":\"Body\",\"statusTag\":\"IN_PROGRESS\"}"
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "liaison_user", roles = {"PUBLIC_SERVANT"})
    void liaisonCanCreateBlogPost() throws Exception {
        mockMvc.perform(
                post("/api/community/blog")
                    .contentType("application/json")
                    .content(
                        "{\"communityId\":\"" + communityId + "\",\"title\":\"Weekly progress\",\"content\":\"Drainage works started\",\"statusTag\":\"IN_PROGRESS\"}"
                    )
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCanReplyToThreadMessageUsingParentMessageId() throws Exception {
        String path = "/api/community/threads/" + threadId + "/messages";
        mockMvc.perform(
                post(path)
                    .contentType("application/json")
                    .content(
                        "{\"sourceCommunityId\":\"" + communityId + "\",\"content\":\"Reply to parent\",\"parentMessageId\":\"" + messageId + "\"}"
                    )
            )
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCanListThreadsWithPagingAndStatusFilter() throws Exception {
        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("sortBy", "RECENT")
                    .param("page", "0")
                    .param("size", "1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].id").value(threadId.toString()));

        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("sortBy", "RECENT")
                    .param("page", "1")
                    .param("size", "1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(recentThreadId.toString()));

        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("status", "ACTIVE")
                    .param("sortBy", "RECENT")
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value(threadId.toString()))
            .andExpect(jsonPath("$.content[1].id").value(recentThreadId.toString()));

        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("status", "STALE")
                    .param("sortBy", "RECENT")
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(staleThreadId.toString()));

        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("sortBy", "RECENT")
                    .param("page", "0")
                    .param("size", "1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(threadId.toString()));
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCanListThreadsSortedByRelevance() throws Exception {
        mockMvc.perform(
                get("/api/community/threads")
                    .param("communityId", communityId.toString())
                    .param("sortBy", "RELEVANCE")
                    .param("page", "0")
                    .param("size", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(recentThreadId.toString()))
            .andExpect(jsonPath("$.content[0].relevanceScore").isNumber())
            .andExpect(jsonPath("$.content[0].totalReactions").value(8))
            .andExpect(jsonPath("$.content[0].totalReplies").value(1))
            .andExpect(jsonPath("$.content[0].messages[0].depth").value(0))
            .andExpect(jsonPath("$.content[0].messages[0].directReplyCount").value(1))
            .andExpect(jsonPath("$.content[0].messages[1].depth").value(1));
    }

    @Test
    @WithMockUser(username = "member_user", roles = {"CITIZEN"})
    void memberCannotReplyBeyondDefinedDepthLimit() throws Exception {
        CommunityThreadMessage levelOne = new CommunityThreadMessage();
        levelOne.setThreadId(threadId);
        levelOne.setAuthorId(memberUserId);
        levelOne.setSourceCommunityId(communityId);
        levelOne.setParentMessageId(messageId);
        levelOne.setContent("Level one");
        levelOne = messageRepository.save(levelOne);

        CommunityThreadMessage levelTwo = new CommunityThreadMessage();
        levelTwo.setThreadId(threadId);
        levelTwo.setAuthorId(memberUserId);
        levelTwo.setSourceCommunityId(communityId);
        levelTwo.setParentMessageId(levelOne.getId());
        levelTwo.setContent("Level two");
        levelTwo = messageRepository.save(levelTwo);

        CommunityThreadMessage levelThree = new CommunityThreadMessage();
        levelThree.setThreadId(threadId);
        levelThree.setAuthorId(memberUserId);
        levelThree.setSourceCommunityId(communityId);
        levelThree.setParentMessageId(levelTwo.getId());
        levelThree.setContent("Level three");
        levelThree = messageRepository.save(levelThree);

        CommunityThreadMessage levelFour = new CommunityThreadMessage();
        levelFour.setThreadId(threadId);
        levelFour.setAuthorId(memberUserId);
        levelFour.setSourceCommunityId(communityId);
        levelFour.setParentMessageId(levelThree.getId());
        levelFour.setContent("Level four");
        levelFour = messageRepository.save(levelFour);

        mockMvc.perform(
                post("/api/community/threads/" + threadId + "/messages")
                    .contentType("application/json")
                    .content(
                        "{\"sourceCommunityId\":\"" + communityId + "\",\"content\":\"Too deep\",\"parentMessageId\":\"" + levelFour.getId() + "\"}"
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Reply depth limit exceeded. Community threads support up to 4 nested reply levels."));
    }

    private User createUser(String username) {
        User user = new User(username, "{noop}pw", username + "@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(true);
        return userRepository.save(user);
    }

    private void addMembership(UUID userId, CommunityRole role, UUID createdBy) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(createdBy);
        membershipRepository.save(membership);
    }
}
