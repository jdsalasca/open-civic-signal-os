package org.opencivic.signalos.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.require-email-verification=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthVerificationBypassIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void registerMarksUserVerifiedAndLoginSucceedsWhenVerificationNotRequired() throws Exception {
        String username = "bypass_user";

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!",
                      "email":"%s@example.com"
                    }
                    """.formatted(username, username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", containsString("successful")))
            .andExpect(jsonPath("$.message", not(containsString("could not be delivered"))))
            .andExpect(jsonPath("$.message", not(containsString("failure"))))
            .andReturn();

        String registerBody = registerResult.getResponse().getContentAsString();
        assertThat(registerBody).doesNotContain("FAILED");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"Passw0rd!"
                    }
                    """.formatted(username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void resendCodeRejectsAccountMarkedVerifiedAtRegistration() throws Exception {
        String username = "bypass_resend_user";
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

        mockMvc.perform(post("/api/auth/resend-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s"}
                    """.formatted(username)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("already verified")));
    }
}
