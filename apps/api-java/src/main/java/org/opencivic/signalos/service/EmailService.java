package org.opencivic.signalos.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationCode(String to, String username, String code) {
        Context context = new Context();
        context.setVariables(Map.of("username", username, "code", code));
        String htmlContent = templateEngine.process("mail/verification", context);
        
        sendHtmlEmail(to, "Signal OS: Verify your Account", htmlContent);
    }

    @Async
    public void sendWelcomeEmail(String to, String username) {
        // En un futuro podrías crear una plantilla 'mail/welcome'
        String body = String.format("Hello %s,\n\nWelcome to Open Civic Signal OS. Your account is now active and ready for civic engagement.", username);
        sendSimpleEmail(to, "Welcome to Signal OS", body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            if (fromEmail == null || fromEmail.isBlank()) {
                log.error("SMTP Configuration Error: spring.mail.username is not set.");
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("✅ Professional HTML Email sent to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Professional Communication failure. To: {}. Reason: {}", to, e.getMessage());
        }
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            log.info("✅ Simple Email sent to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Simple Communication failure. To: {}. Reason: {}", to, e.getMessage());
        }
    }
}
