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
import org.opencivic.signalos.repository.SignalRepository;
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

    private UUID communityId;
    private UUID userId;

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
}
