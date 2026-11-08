package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.UUID;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

/**
 * The onboarding promise of this round: a person registers and leaves attached to a city.
 *
 * <p>Two paths are covered because they are the two ways a community starts. Joining an existing
 * city makes the registrant a member; founding a city makes them its organizer, since a city with no
 * one accountable for it is not a community yet. The public directory is covered here too, because
 * the landing page has to be able to show a city and its size before anyone logs in.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:communitycityonboardingitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityCityOnboardingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void registerWithExistingCityAttachesTheRegistrantAsMember() throws Exception {
        stubMailSender();
        String cityId = communityRepository.findBySlug("central-hub").orElseThrow().getId().toString();
        String username = "joiner_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!",
                      "email":"%s@example.com",
                      "communityId":"%s"
                    }
                    """.formatted(username, username, cityId)))
            .andExpect(status().isOk());

        User user = userRepository.findByUsername(username).orElseThrow();
        CommunityMembership membership = membershipRepository
            .findByUserIdAndCommunityId(user.getId(), UUID.fromString(cityId))
            .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(CommunityRole.MEMBER);
    }

    @Test
    void registerWithNewCityCreatesTheCityAndMakesTheRegistrantOrganizer() throws Exception {
        stubMailSender();
        String cityName = "Valle Verde " + UUID.randomUUID().toString().substring(0, 8);
        String username = "founder_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!",
                      "email":"%s@example.com",
                      "communityName":"%s",
                      "communityCountry":"Colombia",
                      "communityRegion":"Antioquia"
                    }
                    """.formatted(username, username, cityName)))
            .andExpect(status().isOk());

        Community created = communityRepository.findAll().stream()
            .filter(community -> cityName.equals(community.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(created.getCountry()).isEqualTo("Colombia");
        assertThat(created.getRegion()).isEqualTo("Antioquia");
        assertThat(created.getSlug()).isNotBlank();

        User user = userRepository.findByUsername(username).orElseThrow();
        CommunityMembership membership = membershipRepository
            .findByUserIdAndCommunityId(user.getId(), created.getId())
            .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(CommunityRole.COORDINATOR);
    }

    @Test
    void publicCityDirectoryIsReadableWithoutASessionAndReportsMemberCounts() throws Exception {
        mockMvc.perform(get("/api/communities/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.slug == 'rosalistas')].memberCount",
                hasItem(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[?(@.slug == 'rosalistas')].country").exists());
    }

    private void stubMailSender() {
        when(mailSender.createMimeMessage())
            .thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
    }
}
