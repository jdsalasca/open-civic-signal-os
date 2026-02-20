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
            MimeMessage mimeMessage = createEmail(to, "noreply@signalos.org", subject, body);
            Message message = createMessageWithEmail(mimeMessage);
            gmailService.users().messages().send("me", message).execute();
            log.info("Secure civic email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Strategic communication failure. Reason: {}", e.getMessage());
        }
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
