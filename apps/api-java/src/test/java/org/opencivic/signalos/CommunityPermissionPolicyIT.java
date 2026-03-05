package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.repository.CommunityPermissionPolicyRepository;
import org.opencivic.signalos.repository.CommunityRepository;
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
class CommunityPermissionPolicyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityPermissionPolicyRepository policyRepository;

    private String losRosalesId;
    private String centralHubId;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
        Community losRosales = communityRepository.findBySlug("rosalistas").orElseThrow();
        Community centralHub = communityRepository.findBySlug("central-hub").orElseThrow();
        losRosalesId = losRosales.getId().toString();
        centralHubId = centralHub.getId().toString();
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void getPermissionsReturnsDefaultPoliciesForMember() throws Exception {
        mockMvc.perform(get("/api/communities/{communityId}/permissions", losRosalesId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.scope == 'CREATE_THREAD')].allowedRoles[*]",
                Matchers.containsInAnyOrder("COORDINATOR", "MEMBER", "MODERATOR", "PUBLIC_SERVANT_LIAISON")))
            .andExpect(jsonPath("$[?(@.scope == 'MODERATE_THREAD_MESSAGE')].allowedRoles[*]",
                Matchers.containsInAnyOrder("COORDINATOR", "MODERATOR")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN", "PUBLIC_SERVANT", "CITIZEN"})
    void coordinatorCanUpdatePolicies() throws Exception {
        mockMvc.perform(put("/api/communities/{communityId}/permissions", losRosalesId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "policies": [
                        {
                          "scope": "CREATE_THREAD",
                          "allowedRoles": ["COORDINATOR"]
                        },
                        {
                          "scope": "CREATE_OFFICIAL_UPDATE",
                          "allowedRoles": ["COORDINATOR", "PUBLIC_SERVANT_LIAISON"]
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.scope == 'CREATE_THREAD')].allowedRoles[*]",
                Matchers.contains("COORDINATOR")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN", "PUBLIC_SERVANT", "CITIZEN"})
    void restrictedThreadCreationReturnsStructuredPermissionError() throws Exception {
        mockMvc.perform(put("/api/communities/{communityId}/permissions", losRosalesId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "policies": [
                        {
                          "scope": "CREATE_THREAD",
                          "allowedRoles": ["COORDINATOR"]
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sourceCommunityId":"%s",
                      "targetCommunityId":"%s",
                      "title":"Blocked member thread"
                    }
                    """.formatted(losRosalesId, centralHubId))
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("citizen").roles("CITIZEN")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("COMMUNITY_PERMISSION_DENIED"))
            .andExpect(jsonPath("$.permissionScope").value("CREATE_THREAD"))
            .andExpect(jsonPath("$.currentRole").value("MEMBER"))
            .andExpect(jsonPath("$.allowedRoles[0]").value("COORDINATOR"));
    }
}
