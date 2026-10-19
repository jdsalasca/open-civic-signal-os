package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasItem;
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
    "spring.datasource.url=jdbc:h2:mem:helpcenterit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HelpCenterIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User citizen = new User("help_citizen", "encoded", "citizen@example.com", "ROLE_CITIZEN");
        citizen.setVerified(true);
        citizen.setEnabled(true);
        userRepository.save(citizen);

        User moderator = new User("help_mod", "encoded", "mod@example.com", "ROLE_CITIZEN");
        moderator.setVerified(true);
        moderator.setEnabled(true);
        moderator = userRepository.save(moderator);

        Community community = new Community();
        community.setName("Help District");
        community.setSlug("help-district");
        community.setDescription("Onboarding context");
        community = communityRepository.save(community);
        communityId = community.getId();

        membershipRepository.save(membership(moderator.getId(), CommunityRole.MODERATOR, moderator.getId()));
    }

    @Test
    void helpCenterShouldReturnBilingualCitizenGuidesAndSearchResults() throws Exception {
        mockMvc.perform(get("/api/help-center")
                .with(user("help_citizen").roles("CITIZEN"))
                .queryParam("lang", "es")
                .queryParam("surface", "REPORT")
                .queryParam("query", "evidencia"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.persona").value("CITIZEN"))
            .andExpect(jsonPath("$.language").value("es"))
            .andExpect(jsonPath("$.surface").value("REPORT"))
            .andExpect(jsonPath("$.onboardingSteps", hasSize(3)))
            .andExpect(jsonPath("$.guides[0].title").value("Reporte con claridad y evidencia"))
            .andExpect(jsonPath("$.guides[0].surface").value("REPORT"));
    }

    @Test
    void updatingStateShouldPersistDismissedAndCompletedGuides() throws Exception {
        mockMvc.perform(put("/api/help-center/state")
                .with(user("help_mod").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "completedStepKeys": ["moderator-review-safety"],
                      "dismissedGuideKeys": ["moderator-review-safety", "governance-library-basics"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completedStepKeys", hasItem("moderator-review-safety")))
            .andExpect(jsonPath("$.dismissedGuideKeys", hasItem("governance-library-basics")));

        mockMvc.perform(get("/api/help-center")
                .with(user("help_mod").roles("CITIZEN"))
                .header("X-Community-Id", communityId.toString())
                .queryParam("lang", "en")
                .queryParam("surface", "GOVERNANCE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.persona").value("MODERATOR"))
            .andExpect(jsonPath("$.completedStepKeys", hasItem("moderator-review-safety")))
            .andExpect(jsonPath("$.dismissedGuideKeys", hasItem("governance-library-basics")))
            .andExpect(jsonPath("$.onboardingSteps[2].completed").value(true))
            .andExpect(jsonPath("$.onboardingSteps[2].dismissed").value(true))
            .andExpect(jsonPath("$.guides[?(@.id=='governance-library-basics')].dismissed", hasItem(true)));
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
