package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.opencivic.signalos.service.PrioritizationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * The published formula must stay truthful about what the backend actually computes.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:formulait;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PrioritizationFormulaIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void formulaMetadataShouldBePublicAndVersioned() throws Exception {
        mockMvc.perform(get("/api/signals/formula"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v1"))
            .andExpect(jsonPath("$.formula").value(
                org.opencivic.signalos.domain.PrioritizationFormula.expression()))
            .andExpect(jsonPath("$.effectiveFrom").value("2026-02-19"))
            .andExpect(jsonPath("$.weights", hasSize(4)))
            .andExpect(jsonPath("$.cappedFactors", hasSize(2)))
            .andExpect(jsonPath("$.changeNote").isNotEmpty());
    }

    @Test
    void publishedWeightsShouldMatchTheComputation() throws Exception {
        String body = mockMvc.perform(get("/api/signals/formula"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        var formula = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        var weights = formula.get("weights");

        var priorityService = new PrioritizationServiceImpl(null, null, null, null, null);

        // Inputs chosen so both terms actually saturate their published cap:
        // min(3000 / 10, 30) = 30 and min(75 / 5, 15) = 15.
        assertCapMatches(weights, "affectedPeople", 3000, priorityService);
        assertCapMatches(weights, "communityVotes", 75, priorityService);
    }

    @Test
    void publishedVersionShouldMatchTheVersionStampedOnSignals() {
        org.junit.jupiter.api.Assertions.assertEquals(
            PrioritizationServiceImpl.TRANSFORMATION_VERSION,
            org.opencivic.signalos.service.PrioritizationFormulaService.VERSION
        );
    }

    private void assertCapMatches(
        com.fasterxml.jackson.databind.JsonNode weights,
        String factor,
        int rawInput,
        PrioritizationServiceImpl service
    ) {
        double publishedCap = -1;
        for (var weight : weights) {
            if (factor.equals(weight.get("factor").asText())) {
                publishedCap = weight.get("cap").asDouble();
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(publishedCap >= 0, "No published cap for " + factor);

        var signal = new org.opencivic.signalos.domain.Signal();
        signal.setUrgency(1);
        signal.setImpact(1);
        if ("affectedPeople".equals(factor)) {
            signal.setAffectedPeople(rawInput);
        } else {
            signal.setCommunityVotes(rawInput);
        }

        double contribution = "affectedPeople".equals(factor)
            ? service.getBreakdown(signal).affectedPeople()
            : service.getBreakdown(signal).communityVotes();

        org.junit.jupiter.api.Assertions.assertEquals(
            publishedCap,
            contribution,
            0.0001,
            "Published cap for " + factor + " does not match the computed contribution"
        );
    }
}