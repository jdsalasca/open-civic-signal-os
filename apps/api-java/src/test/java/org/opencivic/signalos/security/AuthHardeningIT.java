package org.opencivic.signalos.security;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthHardeningIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void verifyShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    void testEmailEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/test/email").param("to", "audit@example.com"))
            .andExpect(status().isForbidden());
    }

    @Test
    void registerShouldReturnDegradedEmailStatusWhenSmtpFails() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(ArgumentMatchers.any(MimeMessage.class));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"degraded_user",
                      "password":"Passw0rd!",
                      "email":"degraded_user@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.emailDeliveryStatus").value("FAILED"))
            .andExpect(jsonPath("$.supportEmail").exists())
            .andExpect(jsonPath("$.message", containsString("could not be delivered")));
    }
}
