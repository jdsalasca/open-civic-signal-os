package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignalPublicAccessIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void prioritizedEndpointShouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/signals/prioritized?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void top10EndpointShouldAllowAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/signals/top-10"))
            .andExpect(status().isOk());
    }
}
