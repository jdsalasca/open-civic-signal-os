package org.opencivic.signalos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityIntegration;
import org.opencivic.signalos.domain.CommunityIntegrationChannel;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityIntegrationDeliveryRepository;
import org.opencivic.signalos.repository.CommunityIntegrationRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The digest now reaches a channel, and the boundary of what this verifies is worth stating.
 *
 * <p>JavaMailSender is mocked. That verifies the connector resolves the configured recipient,
 * composes the message, and hands it to the mail transport — which is the part this codebase owns.
 * It does not re-verify SMTP itself, which is Spring's and the mail server's job. Claiming more
 * than this would be claiming to have tested something we did not.
 *
 * <p>What it does verify end to end is the property that matters operationally: publishing a week
 * sends once, and publishing the same week again is refused before anything is sent.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:digestdelivery;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.mail.username=no-reply@opencivic.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WeeklyDigestDeliveryIT {

    private static final String WEEK = "2026-W13";
    private static final LocalDate MONDAY = LocalDate.parse("2026-03-23");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private CommunityIntegrationRepository integrationRepository;
    @Autowired private CommunityIntegrationDeliveryRepository deliveryRepository;

    @MockBean private JavaMailSender mailSender;

    private UUID communityId;
    private UUID editorId;

    @BeforeEach
    void setUp() {
        // A real MimeMessage, because the service builds one and the mock would otherwise return null.
        when(mailSender.createMimeMessage())
            .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        doNothing().when(mailSender).send(any(MimeMessage.class));

        User editor = new User("digest_sender", "encoded", "sender@example.com", "ROLE_CITIZEN");
        editor.setVerified(true);
        editor.setEnabled(true);
        editorId = userRepository.save(editor).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Digest delivery");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(editorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(editorId);
        membershipRepository.save(membership);
    }

    @Test
    void publishingShouldSendTheDigestToTheConfiguredAddress() throws Exception {
        signal("Water main break", "utilities", 313.0);
        emailIntegration("digest@neighbourhood.example");

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_sender").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.published").value(true))
            .andExpect(jsonPath("$.deliveredToChannels").value(1));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(sent.capture());

        // The recipient is the integration's configured target, not something invented here.
        assertThat(sent.getValue().getAllRecipients()[0].toString())
            .isEqualTo("digest@neighbourhood.example");
        assertThat(sent.getValue().getSubject()).contains("week");

        assertThat(deliveryRepository.findByCommunityIdOrderByCreatedAtDesc(communityId))
            .hasSize(1);
    }

    @Test
    void publishingTheSameWeekTwiceShouldSendOnlyOnce() throws Exception {
        signal("Water main break", "utilities", 313.0);
        emailIntegration("digest@neighbourhood.example");

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_sender").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk());

        // Residents must not get the same bulletin twice.
        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_sender").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isConflict());

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        assertThat(deliveryRepository.findByCommunityIdOrderByCreatedAtDesc(communityId))
            .hasSize(1);
    }

    @Test
    void aMisconfiguredTargetShouldFailClearlyRatherThanBlowUpThePublish() throws Exception {
        signal("Water main break", "utilities", 313.0);
        // A URL where an address belongs. A community can configure this, so it has to fail well.
        emailIntegration("https://hooks.example.com/digest");

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_sender").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            // Publishing succeeds; the delivery is the thing that fails, and it is recorded.
            .andExpect(jsonPath("$.deliveredToChannels").value(0));

        verify(mailSender, times(0)).send(any(MimeMessage.class));

        var deliveries = deliveryRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getLastError()).contains("not an email address");
    }

    @Test
    void aCommunityWithNoEmailIntegrationShouldPublishWithoutSending() throws Exception {
        signal("Water main break", "utilities", 313.0);

        mockMvc.perform(post("/api/community/weekly-digest/publish")
                .with(user("digest_sender").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("week", WEEK))
            .andExpect(status().isOk())
            // Zero is normal: nobody configured a channel, which is not a failure.
            .andExpect(jsonPath("$.deliveredToChannels").value(0));

        verify(mailSender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    void anEmailIntegrationShouldNotReceiveOrdinaryAnnouncements() throws Exception {
        signal("Water main break", "utilities", 313.0);
        emailIntegration("digest@neighbourhood.example");

        // A webhook-style announcement must not arrive as a weekly bulletin. Residents signed up for
        // one and not the other.
        mockMvc.perform(post("/api/community/integrations/events")
                .with(user("digest_sender").roles("CITIZEN"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "eventType": "OFFICIAL_ANNOUNCEMENT",
                      "title": "Roadworks on Monday",
                      "summary": "Expect delays."
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targeted").value(0));

        verify(mailSender, times(0)).send(any(MimeMessage.class));
    }

    private void emailIntegration(String target) {
        CommunityIntegration integration = new CommunityIntegration();
        integration.setCommunityId(communityId);
        integration.setChannel(CommunityIntegrationChannel.EMAIL_DIGEST);
        integration.setName("Neighbourhood mailing list");
        integration.setTargetUri(target);
        // The column is NOT NULL for every channel, including the ones that do not sign anything.
        // An email digest needs no signing secret; the column simply has no notion of "not applicable".
        integration.setSecretHash(
            org.opencivic.signalos.service.WebhookCommunityIntegrationConnector.hashSecret("unused-for-email"));
        integration.setEnabled(true);
        integration.setAutoRetry(true);
        integration.setCreatedBy(editorId);
        integration.setCreatedAt(LocalDateTime.now());
        integrationRepository.save(integration);
    }

    private UUID signal(String title, String category, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(editorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the digest delivery test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(MONDAY.plusDays(1).atStartOfDay().plusHours(9));
        return signalRepository.save(signal).getId();
    }
}