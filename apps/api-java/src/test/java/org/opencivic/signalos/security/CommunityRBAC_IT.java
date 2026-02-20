package org.opencivic.signalos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
    private UUID messageId;

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

        CommunityThreadMessage message = new CommunityThreadMessage();
        message.setThreadId(threadId);
        message.setAuthorId(memberUserId);
        message.setSourceCommunityId(communityId);
        message.setContent("Initial message");
        message = messageRepository.save(message);
        messageId = message.getId();
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
