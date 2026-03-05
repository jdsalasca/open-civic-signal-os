package org.opencivic.signalos;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.repository.CommunityBlogPostRepository;
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
class CommunityOfficialAnnouncementsIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityBlogPostRepository blogPostRepository;

    private String losRosalesId;

    @BeforeEach
    void setUp() {
        blogPostRepository.deleteAll();
        Community losRosales = communityRepository.findBySlug("rosalistas").orElseThrow();
        losRosalesId = losRosales.getId().toString();
    }

    @Test
    @WithMockUser(username = "servant", roles = {"PUBLIC_SERVANT", "CITIZEN"})
    void pinnedAnnouncementsStayAtTopOfOfficialTimeline() throws Exception {
        mockMvc.perform(post("/api/community/blog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId":"%s",
                      "title":"Road closure this weekend",
                      "content":"Bridge access will close for repairs.",
                      "statusTag":"IN_PROGRESS",
                      "pinned":false
                    }
                    """.formatted(losRosalesId)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/community/blog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId":"%s",
                      "title":"Water service interruption",
                      "content":"Official maintenance window notice.",
                      "statusTag":"PLANNED",
                      "pinned":true
                    }
                    """.formatted(losRosalesId)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/community/blog")
                .param("communityId", losRosalesId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].title").value("Water service interruption"))
            .andExpect(jsonPath("$[0].official").value(true))
            .andExpect(jsonPath("$[0].pinned").value(true))
            .andExpect(jsonPath("$[1].pinned").value(false));
    }

    @Test
    @WithMockUser(username = "servant", roles = {"PUBLIC_SERVANT", "CITIZEN"})
    void archiveEndpointReturnsSearchableArchiveByTextAndDate() throws Exception {
        String response = mockMvc.perform(post("/api/community/blog")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId":"%s",
                      "title":"Campus bus reroute",
                      "content":"Official detour for the north gate.",
                      "statusTag":"PLANNED",
                      "pinned":true
                    }
                    """.formatted(losRosalesId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String postId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/api/community/blog/{postId}/archive", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "archived": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archivedAt").exists())
            .andExpect(jsonPath("$.pinned").value(false));

        String today = java.time.LocalDate.now().toString();

        mockMvc.perform(get("/api/community/blog/archive")
                .param("communityId", losRosalesId)
                .param("query", "reroute")
                .param("dateFrom", today)
                .param("dateTo", today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].title", contains("Campus bus reroute")))
            .andExpect(jsonPath("$[0].official").value(true))
            .andExpect(jsonPath("$[0].archivedAt", containsString(today)));
    }
}
