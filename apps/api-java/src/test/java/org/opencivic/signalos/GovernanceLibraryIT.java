package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GovernanceLibraryIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("governance_coord", "{noop}pw", "governance-coord@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);

        User member = new User("governance_member", "{noop}pw", "governance-member@test.dev", "ROLE_CITIZEN");
        member.setEnabled(true);
        member.setVerified(true);
        member = userRepository.save(member);

        Community community = new Community();
        community.setName("Assembly District");
        community.setSlug("assembly-district");
        community.setDescription("Governance docs scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        CommunityMembership coordinatorMembership = new CommunityMembership();
        coordinatorMembership.setCommunityId(communityId);
        coordinatorMembership.setUserId(coordinator.getId());
        coordinatorMembership.setRole(CommunityRole.COORDINATOR);
        coordinatorMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(coordinatorMembership);

        CommunityMembership memberMembership = new CommunityMembership();
        memberMembership.setCommunityId(communityId);
        memberMembership.setUserId(member.getId());
        memberMembership.setRole(CommunityRole.MEMBER);
        memberMembership.setCreatedBy(coordinator.getId());
        membershipRepository.save(memberMembership);
    }

    @Test
    @WithMockUser(username = "governance_coord", roles = {"CITIZEN"})
    void createsSearchesAndVersionsGovernanceDocuments() throws Exception {
        String documentId = objectMapper.readTree(
            mockMvc.perform(post("/api/community/governance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "communityId", communityId,
                        "title", "Neighborhood assembly agreement",
                        "summary", "Signed agreement for meeting cadence, escalation steps, and documentation duties.",
                        "documentType", "AGREEMENT",
                        "visibility", "COMMUNITY",
                        "tags", java.util.List.of("assembly", "agreement", "meeting"),
                        "content", "Version one defines monthly assembly cadence, quorum minimums, and publication commitments for minutes and decisions.",
                        "changeSummary", "Initial publication of the neighborhood assembly agreement.",
                        "sourceUrl", "https://example.com/governance/agreement-v1"
                    ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").value("AGREEMENT"))
                .andExpect(jsonPath("$.currentVersion.versionNumber").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString()
        ).get("id").asText();

        mockMvc.perform(post("/api/community/governance/{documentId}/versions", documentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "content", "Version two keeps monthly cadence and adds the obligation to publish minutes within five days after each assembly.",
                    "changeSummary", "Added a minutes publication deadline after every assembly meeting.",
                    "sourceUrl", "https://example.com/governance/agreement-v2"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.versionNumber").value(2))
            .andExpect(jsonPath("$.versions[0].changeSummary").value("Added a minutes publication deadline after every assembly meeting."));

        mockMvc.perform(get("/api/community/governance")
                .param("communityId", communityId.toString())
                .param("documentType", "AGREEMENT")
                .param("query", "assembly"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Neighborhood assembly agreement"))
            .andExpect(jsonPath("$[0].versions[0].versionNumber").value(2))
            .andExpect(jsonPath("$[0].tags[0]").value("assembly"));

        mockMvc.perform(get("/api/community/governance/{documentId}", documentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.changeSummary").value("Added a minutes publication deadline after every assembly meeting."))
            .andExpect(jsonPath("$.versions[1].versionNumber").value(1));
    }
}
