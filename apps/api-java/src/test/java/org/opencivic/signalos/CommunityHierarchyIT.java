package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
class CommunityHierarchyIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN", "PUBLIC_SERVANT", "CITIZEN"})
    void createCommunityShouldAcceptParentCommunityIdAndReturnHierarchyFields() throws Exception {
        String parentResponse = mockMvc.perform(post("/api/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"City Campus",
                      "slug":"city-campus",
                      "description":"Top-level campus community"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parentCommunityId").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String parentId = com.jayway.jsonpath.JsonPath.read(parentResponse, "$.id");

        mockMvc.perform(post("/api/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Engineering School",
                      "slug":"engineering-school",
                      "description":"Nested school space",
                      "parentCommunityId":"%s"
                    }
                    """.formatted(parentId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.parentCommunityId").value(parentId));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"SUPER_ADMIN", "PUBLIC_SERVANT", "CITIZEN"})
    void treeAndBreadcrumbEndpointsShouldExposeHierarchy() throws Exception {
        String parentResponse = mockMvc.perform(post("/api/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Metro City",
                      "slug":"metro-city",
                      "description":"Root community"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String parentId = com.jayway.jsonpath.JsonPath.read(parentResponse, "$.id");

        String childResponse = mockMvc.perform(post("/api/communities")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"North District",
                      "slug":"north-district",
                      "description":"Child district",
                      "parentCommunityId":"%s"
                    }
                    """.formatted(parentId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String childId = com.jayway.jsonpath.JsonPath.read(childResponse, "$.id");

        mockMvc.perform(get("/api/communities/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.slug == 'metro-city')]").exists())
            .andExpect(jsonPath("$[?(@.slug == 'metro-city')].children[0].slug").value("north-district"));

        mockMvc.perform(get("/api/communities/{communityId}/breadcrumb", childId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].slug").value("metro-city"))
            .andExpect(jsonPath("$[1].slug").value("north-district"));
    }
}
