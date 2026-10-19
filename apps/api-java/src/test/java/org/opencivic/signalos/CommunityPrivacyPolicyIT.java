package org.opencivic.signalos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:communityprivacyit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityPrivacyPolicyIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("privacy_coord", "encoded", "coord@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinator = userRepository.save(coordinator);

        User member = new User("privacy_member", "encoded", "member@example.com", "ROLE_CITIZEN");
        member.setVerified(true);
        member.setEnabled(true);
        member = userRepository.save(member);

        User target = new User("privacy_target", "encoded", "target@example.com", "ROLE_CITIZEN");
        target.setVerified(true);
        target.setEnabled(true);
        target = userRepository.save(target);

        Community community = new Community();
        community.setName("Policy District");
        community.setSlug("policy-district");
        community.setDescription("Policy scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        membershipRepository.save(membership(coordinator.getId(), CommunityRole.COORDINATOR, coordinator.getId()));
        membershipRepository.save(membership(member.getId(), CommunityRole.MEMBER, coordinator.getId()));
        membershipRepository.save(membership(target.getId(), CommunityRole.MEMBER, coordinator.getId()));
    }

    @Test
    void coordinatorCanUpdateAndReviewCommunityPrivacyPolicy() throws Exception {
        mockMvc.perform(get("/api/auth/profile/privacy_target")
                .with(user("privacy_coord").roles("CITIZEN"))
                .header("X-Community-Id", communityId.toString()))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/communities/{communityId}/privacy", communityId)
                .with(user("privacy_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "openDataPolicy": "AGGREGATED_AND_DECISIONS"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openDataPolicy").value("AGGREGATED_AND_DECISIONS"));

        mockMvc.perform(get("/api/communities/{communityId}/privacy", communityId)
                .with(user("privacy_coord").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityName").value("Policy District"))
            .andExpect(jsonPath("$.recentAccessLogs[0].accessType").value("PROFILE_ADMIN_VIEW"));

        mockMvc.perform(get("/api/communities/{communityId}/privacy", communityId)
                .with(user("privacy_member").roles("CITIZEN")))
            .andExpect(status().isForbidden());
    }

    private CommunityMembership membership(UUID userId, CommunityRole role, UUID createdBy) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(createdBy);
        return membership;
    }
}
