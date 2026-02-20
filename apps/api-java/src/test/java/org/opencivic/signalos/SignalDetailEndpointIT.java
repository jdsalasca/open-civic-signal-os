package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.SignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignalDetailEndpointIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignalRepository signalRepository;

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void shouldReturnSignalByIdWhenExisting() throws Exception {
        signalRepository.deleteAll();

        UUID signalId = UUID.randomUUID();
        signalRepository.save(new Signal(
            signalId,
            "Unsafe crossing near school",
            "Crosswalk paint has faded and vehicles do not slow down.",
            "safety",
            5,
            5,
            90,
            8,
            0.0,
            new ScoreBreakdown(150, 125, 9, 0.8),
            "NEW",
            new ArrayList<>(),
            UUID.randomUUID(),
            LocalDateTime.now().minusMinutes(30)
        ));

        mockMvc.perform(get("/api/signals/{id}", signalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(signalId.toString()))
            .andExpect(jsonPath("$.title").value("Unsafe crossing near school"))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.scoreBreakdown.urgency").value(150.0));
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void shouldReturnNotFoundWhenSignalDoesNotExist() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/signals/{id}", unknownId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Civic signal not found: " + unknownId));
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void shouldReturnBadRequestWhenIdIsMalformed() throws Exception {
        mockMvc.perform(get("/api/signals/not-a-uuid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void shouldReturnNotFoundWhenIdSegmentIsMissing() throws Exception {
        mockMvc.perform(get("/api/signals/"))
            .andExpect(status().isNotFound());
    }
}
