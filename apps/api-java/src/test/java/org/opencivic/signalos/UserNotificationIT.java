package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.UserNotification;
import org.opencivic.signalos.repository.UserNotificationRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:usernotificationitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserNotificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNotificationRepository notificationRepository;

    @Autowired
    private UserNotificationService notificationService;

    private UUID userAId;
    private UUID userBId;
    private UUID unreadForAId;
    private UUID readForAId;
    private UUID notificationForBId;

    @BeforeEach
    void setUp() {
        User userA = new User("notif_a", "{noop}pw", "notif-a@test.dev", "ROLE_CITIZEN");
        userA.setEnabled(true);
        userA.setVerified(true);
        userAId = userRepository.save(userA).getId();

        User userB = new User("notif_b", "{noop}pw", "notif-b@test.dev", "ROLE_CITIZEN");
        userB.setEnabled(true);
        userB.setVerified(true);
        userBId = userRepository.save(userB).getId();

        unreadForAId = notificationRepository.save(
            notification(userAId, "SIGNAL_SUPPORT", "A signal", LocalDateTime.now(), null)
        ).getId();
        readForAId = notificationRepository.save(
            notification(userAId, "THREAD_REPLY", "A thread", LocalDateTime.now().minusHours(1), LocalDateTime.now())
        ).getId();
        notificationForBId = notificationRepository.save(
            notification(userBId, "SIGNAL_STATUS", "B signal", LocalDateTime.now(), null)
        ).getId();
    }

    @Test
    @WithMockUser(username = "notif_a", roles = {"CITIZEN"})
    void inboxIsScopedAndMarkReadUpdatesOnlyTheRecipient() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unread").value(1));

        mockMvc.perform(get("/api/notifications/inbox"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("SIGNAL_SUPPORT"))
            .andExpect(jsonPath("$[1].type").value("THREAD_REPLY"));

        mockMvc.perform(post("/api/notifications/{id}/read", unreadForAId))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    @WithMockUser(username = "notif_a", roles = {"CITIZEN"})
    void cannotReadAnotherRecipientsNotification() throws Exception {
        mockMvc.perform(post("/api/notifications/{id}/read", notificationForBId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "notif_a", roles = {"CITIZEN"})
    void markAllReadClearsOnlyTheInboxOfTheCaller() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count"))
            .andExpect(jsonPath("$.unread").value(0));

        UserNotification other = notificationRepository.findById(notificationForBId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(other.getReadAt()).isNull();
    }

    @Test
    void notifySkipsSelfButStoresForAnotherRecipient() {
        notificationService.notifyUser(
            userAId, userAId, "notif_a", "SIGNAL_SUPPORT", "SIGNAL", UUID.randomUUID(), "Self", null
        );
        notificationService.notifyUser(
            userAId, userBId, "notif_b", "THREAD_REPLY", "THREAD", UUID.randomUUID(), "From B", null
        );

        org.assertj.core.api.Assertions.assertThat(
            notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userAId)
        ).hasSize(3);
    }

    private UserNotification notification(
        UUID userId,
        String type,
        String title,
        LocalDateTime createdAt,
        LocalDateTime readAt
    ) {
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setActorName("neighbor");
        notification.setType(type);
        notification.setTargetType("SIGNAL");
        notification.setTargetId(UUID.randomUUID());
        notification.setTargetTitle(title);
        notification.setCreatedAt(createdAt);
        notification.setReadAt(readAt);
        return notification;
    }
}
