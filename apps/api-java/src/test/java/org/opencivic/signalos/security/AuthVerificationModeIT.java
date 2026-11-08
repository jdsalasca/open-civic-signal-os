package org.opencivic.signalos.security;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthVerificationModeIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void registerKeepsUserUnverifiedAndLoginRejectedWhenVerificationRequired() throws Exception {
        String username = "verify_required_user";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!",
                      "email":"%s@example.com"
                    }
                    """.formatted(username, username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailDeliveryStatus").exists());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!"
                    }
                    """.formatted(username)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("not verified")));
    }

    @Test
    void registerWithMalformedJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": broken"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("malformed")));
    }

    @Test
    void loginRejectsWrongPasswordEvenWhenAccountActive() throws Exception {
        String username = "verify_required_wrong_pass";
        Mockito.when(mailSender.createMimeMessage())
            .thenReturn(new MimeMessage(jakarta.mail.Session.getDefaultInstance(new Properties())));
        List<String> capturedBodies = new CopyOnWriteArrayList<>();
        Mockito.doAnswer(invocation -> {
            MimeMessage message = invocation.getArgument(0);
            capturedBodies.addAll(extractTextParts(message));
            return null;
        }).when(mailSender).send(ArgumentMatchers.any(MimeMessage.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!",
                      "email":"%s@example.com"
                    }
                    """.formatted(username, username)))
            .andExpect(status().isOk());

        String verificationCode = extractVerificationCode(capturedBodies);

        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","code":"%s"}
                    """.formatted(username, verificationCode)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"WrongPassw0rd!"
                    }
                    """.formatted(username)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message", containsString("Invalid credentials")));
    }

    private static String extractVerificationCode(List<String> capturedBodies) {
        Pattern pattern = Pattern.compile("code-box[^>]*>\\s*(\\d{6})");
        for (String body : capturedBodies) {
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new IllegalStateException("No verification code found in captured messages: " + capturedBodies);
    }

    private static List<String> extractTextParts(MimeMessage message) throws MessagingException, java.io.IOException {
        Object content = message.getContent();
        if (content instanceof MimeMultipart multipart) {
            List<String> parts = new java.util.ArrayList<>();
            for (int i = 0; i < multipart.getCount(); i++) {
                parts.addAll(extractTextParts(multipart.getBodyPart(i)));
            }
            return parts;
        }
        if (content instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private static List<String> extractTextParts(jakarta.mail.BodyPart bodyPart) throws MessagingException, java.io.IOException {
        Object content = bodyPart.getContent();
        if (content instanceof MimeMultipart multipart) {
            List<String> parts = new java.util.ArrayList<>();
            for (int i = 0; i < multipart.getCount(); i++) {
                parts.addAll(extractTextParts(multipart.getBodyPart(i)));
            }
            return parts;
        }
        if (content instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }
}
