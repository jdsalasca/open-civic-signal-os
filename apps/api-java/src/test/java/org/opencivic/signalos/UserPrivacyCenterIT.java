package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
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
import org.opencivic.signalos.domain.InterfaceMode;
import org.opencivic.signalos.domain.ProfileVisibility;
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
    "spring.datasource.url=jdbc:h2:mem:userprivacyit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPrivacyCenterIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User owner = new User("privacy_owner", "encoded", "owner@example.com", "ROLE_CITIZEN");
        owner.setVerified(true);
        owner.setEnabled(true);
        owner.setDisplayName("Ana Rivera");
        owner.setCivicRole("STUDENT");
        owner.setBio("Tracks neighborhood follow-up.");
        owner.setAffiliations(java.util.List.of("Central Campus", "Neighborhood 7"));
        owner.setProfileVisibility(ProfileVisibility.COMMUNITY);
        owner.setAffiliationVisibility(ProfileVisibility.ADMINS);
        owner.setActivityVisibility(ProfileVisibility.COMMUNITY);
        owner.setInterfaceMode(InterfaceMode.ADVANCED);
        owner = userRepository.save(owner);

        User admin = new User("privacy_admin", "encoded", "admin@example.com", "ROLE_PUBLIC_SERVANT");
        admin.setVerified(true);
        admin.setEnabled(true);
        admin = userRepository.save(admin);

        Community community = new Community();
        community.setName("Privacy District");
        community.setSlug("privacy-district");
        community.setDescription("Privacy scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        membershipRepository.save(membership(owner.getId(), CommunityRole.MEMBER, owner.getId()));
        membershipRepository.save(membership(admin.getId(), CommunityRole.COORDINATOR, owner.getId()));
    }

    @Test
    void updatePrivacySettingsAndReviewAccessLogs() throws Exception {
        mockMvc.perform(put("/api/auth/profile/me")
                .with(user("privacy_owner").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName": "Ana Maria Rivera",
                      "civicRole": "TEACHER",
                      "bio": "Coordinates reading circles and reports service issues.",
                      "affiliations": ["Central Campus", "Library Committee"],
                      "profileVisibility": "COMMUNITY",
                      "affiliationVisibility": "ADMINS",
                      "activityVisibility": "ADMINS",
                      "interfaceMode": "SIMPLE",
                      "avatarPreset": "harbor-light"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activityVisibility").value("ADMINS"))
            .andExpect(jsonPath("$.achievements", hasSize(6)));

        mockMvc.perform(get("/api/auth/profile/privacy_owner")
                .with(user("privacy_admin").roles("PUBLIC_SERVANT"))
                .header("X-Community-Id", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.affiliations[0]").value("Central Campus"))
            .andExpect(jsonPath("$.achievements[0].key").value("VERIFIED_MEMBER"));

        mockMvc.perform(get("/api/auth/privacy/access-logs")
                .with(user("privacy_owner").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accessType").value("PROFILE_ADMIN_VIEW"))
            .andExpect(jsonPath("$[0].actorUsername").value("privacy_admin"))
            .andExpect(jsonPath("$[0].communityId").value(communityId.toString()))
            .andExpect(jsonPath("$[0].note").value(org.hamcrest.Matchers.containsString("email")));
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
