package org.opencivic.signalos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcomeEmail(String to, String username) {
        log.info("Sending async welcome email to user: {}", username);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@signalos.org");
            message.setTo(to);
            message.setSubject("Welcome to Signal OS");
            message.setText("Hello " + username + ",\n\nYour civic account has been created successfully. Welcome to the platform!");
            mailSender.send(message);
            log.info("Welcome email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send welcome email to {}. Reason: {}", to, e.getMessage());
        }
    }
}
