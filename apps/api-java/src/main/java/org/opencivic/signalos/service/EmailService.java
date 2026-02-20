package org.opencivic.signalos.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired(required = false)
    private Gmail gmailService;

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
        if (gmailService == null) {
            log.warn("DRY RUN: Gmail Service not configured. To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }

        try {
            // 3. Create the message (MimeMessage)
            jakarta.mail.internet.MimeMessage mimeMessage = new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getDefaultInstance(new java.util.Properties()));
            mimeMessage.setFrom(new jakarta.mail.internet.InternetAddress("open-civic@n8n-workflows-468303.iam.gserviceaccount.com"));
            mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new jakarta.mail.internet.InternetAddress(to));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(body);
            
            // 4. Encode and send
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            String encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
            
            com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
            message.setRaw(encodedEmail);
            
            gmailService.users().messages().send("me", message).execute();
            log.info("✅ Secure civic email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Strategic communication failure. Reason: {}. Check if Gmail API is enabled for project n8n-workflows-468303.", e.getMessage());
        }
    }
}
