package org.opencivic.signalos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "application.mail.enabled=false")
@ActiveProfiles("test")
class EmailServiceDisabledTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private NotificationRepository notificationRepository;

    @Test
    void disabledMailReturnsClearFailureWithoutContactingSmtpOrAuditingAnOutage() {
        EmailDeliveryResult result = emailService.sendWelcomeEmail("resident@example.org", "resident");

        assertFalse(result.delivered());
        assertEquals("Email delivery is disabled by configuration.", result.failureReason());
        verifyNoInteractions(mailSender, notificationRepository);
    }

    @Test
    void disabledMailSkipsHtmlVerificationMessagesBeforeContactingSmtp() {
        EmailDeliveryResult result = emailService.sendVerificationCode("resident@example.org", "resident", "123456");

        assertFalse(result.delivered());
        assertEquals("Email delivery is disabled by configuration.", result.failureReason());
        verifyNoInteractions(mailSender, notificationRepository);
    }
}
