package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PrioritizedMetricsEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "superadmin", roles = {"SUPER_ADMIN"})
    void shouldExposePrioritizedMetricsAfterRequest() throws Exception {
        mockMvc.perform(get("/api/signals/prioritized?page=0&size=5"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/metrics/signalos.prioritized.latency"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("signalos.prioritized.latency"));

        mockMvc.perform(get("/actuator/metrics/signalos.prioritized.requests.total"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("signalos.prioritized.requests.total"));
    }
}
