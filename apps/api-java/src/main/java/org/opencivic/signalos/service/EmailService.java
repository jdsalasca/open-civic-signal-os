package org.opencivic.signalos.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String to, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@opencivic.org");
        message.setTo(to);
        message.setSubject("Welcome to Open Civic Signal OS");
        message.setText("""
                Hello %s,

                Your account has been successfully registered. You can now contribute by reporting and voting on civic issues.

                Best regards,
                Open Civic Team
                """.formatted(username));
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Log error but don't block registration for now
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
