package org.opencivic.signalos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.opencivic.signalos.repository.UserRepository;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationCode(String to, String username, String code) {
        String subject = "Signal OS: Verify your Account";
        String body = String.format(
            "Hello %s,\n\nYour civic activation code is: %s\n\nPlease enter this code in the OS portal to verify your identity.",
            username, code
        );
        sendEmail(to, subject, body);
    }

    @Async
    public void sendGovernmentalNotification(String to, String title, String content) {
        String subject = "URGENT: Civic Intelligence Relay - " + title;
        String body = "Protocol Alert from Signal OS Intelligence Feed:\n\n" + content;
        sendEmail(to, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            if (fromEmail == null || fromEmail.isBlank()) {
                log.error("SMTP Configuration Error: spring.mail.username is not set.");
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("✅ SMTP Secure Email sent to: {} from: {}", to, fromEmail);
        } catch (Exception e) {
            log.error("CRITICAL: SMTP Communication failure. To: {}. Reason: {}", to, e.getMessage(), e);
        }
    }
}
