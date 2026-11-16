package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.CommunityThread;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityBlogPostRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityThreadMessageRepository;
import org.opencivic.signalos.repository.CommunityThreadRepository;
import org.opencivic.signalos.repository.UserNotificationRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.opencivic.signalos.service.CommunityCollaborationService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:communityhomeitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityHomeIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private CommunityBlogPostRepository blogPostRepository;

    @Autowired
    private CommunityThreadRepository threadRepository;

    @Autowired
    private CommunityThreadMessageRepository messageRepository;

    @Autowired
    private SignalRepository signalRepository;

    @Autowired
    private CommunityCollaborationService collaborationService;

    @Autowired
    private UserNotificationRepository notificationRepository;

    private UUID communityId;
    private UUID userId;
    private UUID threadId;

    @BeforeEach
    void setUp() {
        User user = new User("community_home_user", "{noop}pw", "community-home@test.dev", "ROLE_CITIZEN");
        user.setEnabled(true);
        user.setVerified(true);
        user = userRepository.save(user);
        userId = user.getId();

        Community community = new Community();
        community.setName("Community Home District");
        community.setSlug("community-home-district");
        community.setDescription("Composite home test");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);

        User replier = new User("thread_replier", "{noop}pw", "thread-replier@test.dev", "ROLE_CITIZEN");
        replier.setEnabled(true);
        replier.setVerified(true);
        replier = userRepository.save(replier);
        CommunityMembership replierMembership = new CommunityMembership();
        replierMembership.setCommunityId(communityId);
        replierMembership.setUserId(replier.getId());
        replierMembership.setRole(CommunityRole.MEMBER);
        replierMembership.setCreatedBy(userId);
        membershipRepository.save(replierMembership);

        CommunityBlogPost post = new CommunityBlogPost();
        post.setCommunityId(communityId);
        post.setAuthorId(userId);
        post.setOfficial(true);
        post.setPinned(true);
        post.setTitle("Official resurfacing update");
        post.setContent("Work starts this week");
        post.setStatusTag("IN_PROGRESS");
        post.setPublishedAt(LocalDateTime.now().minusHours(2));
        post.setUpdatedAt(LocalDateTime.now().minusHours(2));
        blogPostRepository.save(post);

        CommunityThread thread = new CommunityThread();
        thread.setSourceCommunityId(communityId);
        thread.setTargetCommunityId(communityId);
        thread.setTitle("Bus stop safety discussion");
        thread.setCreatedBy(userId);
        thread.setCreatedAt(LocalDateTime.now().minusHours(4));
        thread.setUpdatedAt(LocalDateTime.now().minusMinutes(30));
        thread = threadRepository.save(thread);
        threadId = thread.getId();

        CommunityThreadMessage root = new CommunityThreadMessage();
        root.setThreadId(thread.getId());
        root.setAuthorId(userId);
        root.setSourceCommunityId(communityId);
        root.setContent("Need better lighting near the stop");
        root.setReactions(Map.of("🔥", 4, "👍", 2));
        root = messageRepository.save(root);

        CommunityThreadMessage reply = new CommunityThreadMessage();
        reply.setThreadId(thread.getId());
        reply.setAuthorId(userId);
        reply.setSourceCommunityId(communityId);
        reply.setParentMessageId(root.getId());
        reply.setContent("Agreed, especially after 7pm");
        messageRepository.save(reply);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("Broken crosswalk light");
        signal.setDescription("Crosswalk light is off during peak hours.");
        signal.setCategory("INFRASTRUCTURE");
        signal.setStatus("NEW");
        signal.setLocationLabel("Main crosswalk");
        signal.setCommunityId(communityId);
        signal.setAuthorId(userId);
        signal.setCreatedAt(LocalDateTime.now().minusHours(3));
        signal.setPriorityScore(245.0);
        signalRepository.save(signal);
    }

    @Test
    @WithMockUser(username = "community_home_user", roles = {"CITIZEN"})
    void returnsUnifiedCommunityHomePayload() throws Exception {
        mockMvc.perform(
                get("/api/community/home")
                    .param("communityId", communityId.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityId").value(communityId.toString()))
            .andExpect(jsonPath("$.generatedAt").exists())
            .andExpect(jsonPath("$.freshness").isString())
            .andExpect(jsonPath("$.officialUpdates.length()").value(1))
            .andExpect(jsonPath("$.officialUpdates[0].title").value("Official resurfacing update"))
            .andExpect(jsonPath("$.hotThreads.length()").value(1))
            .andExpect(jsonPath("$.hotThreads[0].title").value("Bus stop safety discussion"))
            .andExpect(jsonPath("$.hotThreads[0].relevanceScore").isNumber())
            .andExpect(jsonPath("$.topSignals.length()").value(1))
            .andExpect(jsonPath("$.topSignals[0].title").value("Broken crosswalk light"));
    }

    @Test
    @WithMockUser(username = "community_home_user", roles = {"CITIZEN"})
    void feedUsesReadableSummaries() throws Exception {
        mockMvc.perform(
                get("/api/community/feed")
                    .param("communityId", communityId.toString())
                    .param("days", "7")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("thread-update"))
            .andExpect(jsonPath("$[0].summary").value("Need better lighting near the stop"))
            .andExpect(jsonPath("$[1].type").value("blog"))
            .andExpect(jsonPath("$[1].summary").value("Work starts this week"))
            .andExpect(jsonPath("$[2].type").value("signal"))
            .andExpect(jsonPath("$[2].summary").value("Main crosswalk"));
    }

    @Test
    @WithMockUser(username = "community_home_user", roles = {"CITIZEN"})
    void replyingToAThreadNotifiesItsCreator() {
        collaborationService.addMessage(threadId, communityId, "A new reply", null, "thread_replier");

        assertThat(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId))
            .anyMatch(notification -> "THREAD_REPLY".equals(notification.getType())
                && threadId.equals(notification.getTargetId()));
    }
}
