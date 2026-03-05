package org.opencivic.signalos.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileVisibilityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        communityRepository.deleteAll();
        userRepository.deleteAll();

        Community community = new Community();
        community.setName("Central Campus");
        community.setSlug("central-campus");
        community.setDescription("Shared community");
        communityId = communityRepository.save(community).getId();

        User owner = new User("profile_owner", "encoded", "owner@example.com", "ROLE_CITIZEN");
        owner.setVerified(true);
        owner.setEnabled(true);
        owner.setDisplayName("Ana Rivera");
        owner.setCivicRole("STUDENT");
        owner.setBio("Community organizer and student reporter.");
        owner.setAffiliations(java.util.List.of("Central Campus", "Neighborhood 7"));
        owner.setProfileVisibility(ProfileVisibility.COMMUNITY);
        owner.setAffiliationVisibility(ProfileVisibility.ADMINS);
        owner.setInterfaceMode(InterfaceMode.ADVANCED);
        owner = userRepository.save(owner);

        User viewer = new User("community_viewer", "encoded", "viewer@example.com", "ROLE_CITIZEN");
        viewer.setVerified(true);
        viewer.setEnabled(true);
        viewer = userRepository.save(viewer);

        User admin = new User("community_admin", "encoded", "admin@example.com", "ROLE_PUBLIC_SERVANT");
        admin.setVerified(true);
        admin.setEnabled(true);
        admin = userRepository.save(admin);

        membershipRepository.save(membership(owner.getId(), CommunityRole.MEMBER, owner.getId()));
        membershipRepository.save(membership(viewer.getId(), CommunityRole.MEMBER, owner.getId()));
        membershipRepository.save(membership(admin.getId(), CommunityRole.COORDINATOR, owner.getId()));
    }

    @Test
    @WithMockUser(username = "profile_owner", roles = {"CITIZEN"})
    void updateProfileShouldPersistIdentityAndVisibilitySettings() throws Exception {
        mockMvc.perform(put("/api/auth/profile/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "displayName": "Ana Maria Rivera",
                      "civicRole": "TEACHER",
                      "bio": "Coordinates reading circles and reports service issues.",
                      "affiliations": ["Central Campus", "Library Committee"],
                      "profileVisibility": "COMMUNITY",
                      "affiliationVisibility": "ADMINS",
                      "interfaceMode": "SIMPLE"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Ana Maria Rivera"))
            .andExpect(jsonPath("$.civicRole").value("TEACHER"))
            .andExpect(jsonPath("$.affiliations[0]").value("Central Campus"))
            .andExpect(jsonPath("$.affiliationVisibility").value("ADMINS"))
            .andExpect(jsonPath("$.interfaceMode").value("SIMPLE"))
            .andExpect(jsonPath("$.viewerScope").value("ADMINS"))
            .andExpect(jsonPath("$.email").value("owner@example.com"));
    }

    @Test
    void anonymousViewerShouldOnlyReceivePublicProfileFields() throws Exception {
        mockMvc.perform(get("/api/auth/profile/profile_owner"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("profile_owner"))
            .andExpect(jsonPath("$.displayName").value("profile_owner"))
            .andExpect(jsonPath("$.civicRole").value(nullValue()))
            .andExpect(jsonPath("$.bio").value(nullValue()))
            .andExpect(jsonPath("$.affiliations").isArray())
            .andExpect(jsonPath("$.affiliations").isEmpty())
            .andExpect(jsonPath("$.viewerScope").value("PUBLIC"));
    }

    @Test
    @WithMockUser(username = "community_viewer", roles = {"CITIZEN"})
    void sharedCommunityViewerShouldReceiveCommunityVisibleFieldsOnly() throws Exception {
        mockMvc.perform(get("/api/auth/profile/profile_owner")
                .header("X-Community-Id", communityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Ana Rivera"))
            .andExpect(jsonPath("$.civicRole").value("STUDENT"))
            .andExpect(jsonPath("$.bio").value("Community organizer and student reporter."))
            .andExpect(jsonPath("$.affiliations").isArray())
            .andExpect(jsonPath("$.affiliations").isEmpty())
            .andExpect(jsonPath("$.viewerScope").value("COMMUNITY"));
    }

    @Test
    @WithMockUser(username = "community_admin", roles = {"PUBLIC_SERVANT"})
    void communityAdminViewerShouldReceiveAdminVisibleFields() throws Exception {
        mockMvc.perform(get("/api/auth/profile/profile_owner")
                .header("X-Community-Id", communityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Ana Rivera"))
            .andExpect(jsonPath("$.civicRole").value("STUDENT"))
            .andExpect(jsonPath("$.affiliations[0]").value("Central Campus"))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.viewerScope").value("ADMINS"));
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
