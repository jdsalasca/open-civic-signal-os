package org.opencivic.signalos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit trail metadata: every ranked output must be attributable to an ingest channel
 * and to the scoring rule version that produced it.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auditit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalAuditMetadataIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User citizen = new User("audit_citizen", "encoded", "audit@example.com", "ROLE_CITIZEN");
        citizen.setVerified(true);
        citizen.setEnabled(true);
        userRepository.save(citizen);
    }

    @Test
    void defaultChannelAndTransformationVersionShouldBeStamped() throws Exception {
        String signalId = createSignal(null, null);

        mockMvc.perform(get("/api/signals/{id}", signalId)
                .with(user("audit_citizen").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceChannel").value("WEB_FORM"))
            .andExpect(jsonPath("$.transformationVersion").value("v1"))
            .andExpect(jsonPath("$.sourceRef").doesNotExist());
    }

    @Test
    void explicitIngestChannelShouldBePreservedOnRankedOutput() throws Exception {
        String signalId = createSignal("CSV_IMPORT", "rows/2026-03-24.csv#142");

        mockMvc.perform(get("/api/signals/prioritized")
                .with(user("audit_citizen").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.id=='" + signalId + "')].sourceChannel").value(org.hamcrest.Matchers.hasItem("CSV_IMPORT")))
            .andExpect(jsonPath("$.content[?(@.id=='" + signalId + "')].transformationVersion")
                .value(org.hamcrest.Matchers.hasItem("v1")));
    }

    @Test
    void institutionalUpdateShouldBeDistinguishableFromCitizenReport() throws Exception {
        String citizenReport = createSignal(null, null);
        String institutional = createSignal("INSTITUTIONAL_UPDATE", "municipal-ticket/88213");

        mockMvc.perform(get("/api/signals/{id}", institutional)
                .with(user("audit_citizen").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceChannel").value("INSTITUTIONAL_UPDATE"))
            .andExpect(jsonPath("$.sourceRef").value("municipal-ticket/88213"));

        mockMvc.perform(get("/api/signals/{id}", citizenReport)
                .with(user("audit_citizen").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceChannel").value("WEB_FORM"));
    }

    @Test
    void unknownIngestChannelShouldBeRejectedNotSilentlyDefaulted() throws Exception {
        mockMvc.perform(post("/api/signals")
                .with(user("audit_citizen").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("CARRIER_PIGEON", null)))
            .andExpect(status().isBadRequest());
    }

    private String createSignal(String sourceChannel, String sourceRef) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/signals")
                .with(user("audit_citizen").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(sourceChannel, sourceRef)))
            .andExpect(status().isOk())
            .andReturn();

        return new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createBody(String sourceChannel, String sourceRef) {
        String extra = "";
        if (sourceChannel != null) {
            extra += "\"sourceChannel\": \"" + sourceChannel + "\",";
        }
        if (sourceRef != null) {
            extra += "\"sourceRef\": \"" + sourceRef + "\",";
        }
        return """
            {
              "title": "Streetlight outage on the main corridor",
              "description": "Main corridor lights are out for three consecutive nights.",
              "category": "infrastructure",
              "urgency": 3,
              "impact": 3,
              "affectedPeople": 120,
              %s
              "latitude": null,
              "longitude": null
            }
            """.formatted(extra);
    }
}