package org.opencivic.signalos.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencivic.signalos.domain.Notification;
import org.opencivic.signalos.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.mail.username:no-reply@localhost}")
    private String fromEmail;

    public EmailDeliveryResult sendVerificationCode(String to, String username, String code) {
        Context context = new Context();
        context.setVariables(Map.of("username", username, "code", code));
        String htmlContent = templateEngine.process("mail/verification", context);
        
        return sendHtmlEmail(to, "Signal OS: Verify your Account", htmlContent, "REGISTER_VERIFY");
    }

    public EmailDeliveryResult sendWelcomeEmail(String to, String username) {
        String body = String.format("Hello %s,\n\nWelcome to Open Civic Signal OS. Your account is now active and ready for civic engagement.", username);
        return sendSimpleEmail(to, "Welcome to Signal OS", body, "WELCOME");
    }

    private EmailDeliveryResult sendHtmlEmail(String to, String subject, String htmlBody, String flow) {
        try {
            if (fromEmail == null || fromEmail.isBlank()) {
                String reason = "SMTP Configuration Error: spring.mail.username is not set.";
                log.error(reason);
                auditEmailFailure(flow, to, reason);
                return EmailDeliveryResult.failed(reason);
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("✅ Professional HTML Email sent to: {}", to);
            return EmailDeliveryResult.success();
        } catch (Exception e) {
            String reason = e.getMessage() == null ? "unknown error" : e.getMessage();
            log.error("CRITICAL: Professional Communication failure. To: {}. Reason: {}", to, reason);
            auditEmailFailure(flow, to, reason);
            return EmailDeliveryResult.failed(reason);
        }
    }

    private EmailDeliveryResult sendSimpleEmail(String to, String subject, String body, String flow) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            log.info("✅ Simple Email sent to: {}", to);
            return EmailDeliveryResult.success();
        } catch (Exception e) {
            String reason = e.getMessage() == null ? "unknown error" : e.getMessage();
            log.error("CRITICAL: Simple Communication failure. To: {}. Reason: {}", to, reason);
            auditEmailFailure(flow, to, reason);
            return EmailDeliveryResult.failed(reason);
        }
    }

    private void auditEmailFailure(String flow, String to, String reason) {
        String maskedRecipient = maskEmail(to);
        String truncatedReason = reason.length() > 220 ? reason.substring(0, 220) : reason;
        Notification notification = new Notification(
            "EMAIL_FAILURE",
            String.format("flow=%s recipient=%s reason=%s", flow, maskedRecipient, truncatedReason),
            "AUTH_PIPELINE",
            LocalDateTime.now()
        );
        notificationRepository.save(notification);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }
}
