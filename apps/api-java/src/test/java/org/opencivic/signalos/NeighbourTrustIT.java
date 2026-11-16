package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserNotificationRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:neighbourtrustitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NeighbourTrustIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private SignalRepository signalRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    private UUID authorId;
    private UUID neighbourId;
    private UUID communityId;
    private UUID signalId;

    @BeforeEach
    void setUp() {
        User author = new User("trust_author", "{noop}pw", "trust-author@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        authorId = userRepository.save(author).getId();

        User neighbour = new User("trust_neighbour", "{noop}pw", "trust-neighbour@test.dev", "ROLE_CITIZEN");
        neighbour.setEnabled(true);
        neighbour.setVerified(true);
        neighbourId = userRepository.save(neighbour).getId();

        Community community = new Community();
        community.setName("Trust District");
        community.setSlug("trust-district");
        community.setDescription("Neighbour trust test");
        communityId = communityRepository.save(community).getId();

        membership(authorId);
        membership(neighbourId);

        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setTitle("Street light is out");
        signal.setDescription("The corner is dark after 7pm.");
        signal.setCategory("INFRASTRUCTURE");
        signal.setStatus("NEW");
        signal.setCommunityId(communityId);
        signal.setAuthorId(authorId);
        signal.setLocationLabel("3rd street");
        signal.setCreatedAt(LocalDateTime.now().minusHours(1));
        signal.setPriorityScore(120.0);
        signalId = signalRepository.save(signal).getId();
    }

    @Test
    @WithMockUser(username = "trust_neighbour", roles = {"CITIZEN"})
    void followAndUnfollowAreIdempotent() throws Exception {
        mockMvc.perform(post("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(post("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(get("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(jsonPath("$.following").value(true));
        mockMvc.perform(delete("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(jsonPath("$.following").value(false));
        mockMvc.perform(get("/api/follows/SIGNAL/{id}", signalId))
            .andExpect(jsonPath("$.following").value(false));
    }

    @Test
    @WithMockUser(username = "trust_neighbour", roles = {"CITIZEN"})
    void confirmationIsCountedOnceAndNotifiesTheAuthor() throws Exception {
        mockMvc.perform(get("/api/signals/{id}/confirmations", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.confirmedByViewer").value(false));

        mockMvc.perform(post("/api/signals/{id}/confirmations", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.confirmedByViewer").value(true));

        mockMvc.perform(post("/api/signals/{id}/confirmations", signalId))
            .andExpect(jsonPath("$.count").value(1));

        assertThat(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(authorId))
            .anyMatch(notification -> "SIGNAL_CONFIRMATION".equals(notification.getType())
                && signalId.equals(notification.getTargetId()));

        mockMvc.perform(delete("/api/signals/{id}/confirmations", signalId))
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.confirmedByViewer").value(false));
    }

    private void membership(UUID userId) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);
    }
}
