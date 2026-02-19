package org.opencivic.signalos.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityRBAC_IT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void citizenShouldNotAccessModerationQueue() throws Exception {
        mockMvc.perform(get("/api/signals/flagged"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void citizenShouldNotUpdateStatus() throws Exception {
        String url = String.format("/api/signals/%s/status", UUID.randomUUID());
        mockMvc.perform(patch(url)
                .contentType("application/json")
                .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff", roles = {"PUBLIC_SERVANT"})
    void staffShouldAccessModerationQueue() throws Exception {
        mockMvc.perform(get("/api/signals/flagged"))
                .andExpect(status().isOk());
    }
}
