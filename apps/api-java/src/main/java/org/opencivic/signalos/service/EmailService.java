package org.opencivic.signalos.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async // BE-P2-10: Truly non-blocking email sending
    public void sendWelcomeEmail(String to, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@signalos.org");
            message.setTo(to);
            message.setSubject("Welcome to Signal OS");
            message.setText("Hello " + username + ",\n\nYour civic account has been created successfully. Welcome to the platform!");
            mailSender.send(message);
        } catch (Exception e) {
            // BE-P2-10: Structured logging instead of silent failure
            System.err.println("CRITICAL: Failed to send welcome email to " + to + ". Reason: " + e.getMessage());
        }
    }
}
