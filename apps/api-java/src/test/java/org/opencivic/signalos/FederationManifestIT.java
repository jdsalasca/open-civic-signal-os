package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityOpenDataPolicy;
import org.opencivic.signalos.repository.CommunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * City-to-city compatibility fails in a specific way: two instances running different versions each
 * assume the other speaks their dialect, and the mismatch surfaces as a parse error deep in an
 * importer rather than as a clear refusal.
 *
 * <p>The manifest makes the contract explicit and checkable before anything is exchanged.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:federation;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.instance-name=Riverside Instance"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FederationManifestIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CommunityRepository communityRepository;

    @Test
    void theManifestShouldBeReadableWithoutAToken() throws Exception {
        // A peer has to read the contract before it can decide whether to ask for a token.
        mockMvc.perform(get("/api/federation/manifest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instanceName").value("Riverside Instance"))
            .andExpect(jsonPath("$.contractVersion").value("v1"))
            .andExpect(jsonPath("$.supportedContractVersions", hasSize(1)))
            .andExpect(jsonPath("$.datasets", hasSize(6)))
            .andExpect(jsonPath("$.datasets[0].exportType").value("SIGNALS"))
            .andExpect(jsonPath("$.datasets[0].contractVersion").value("v1"))
            .andExpect(jsonPath("$.authentication").value(
                containsString("X-Api-Token")))
            .andExpect(jsonPath("$.rateLimitHeader").value(
                containsString("X-RateLimit-Limit")));
    }

    @Test
    void theManifestShouldNotPromiseThatAPeerSpeaksTheSameContract() throws Exception {
        mockMvc.perform(get("/api/federation/manifest"))
            .andExpect(status().isOk())
            // A peer reading "contractVersion v1" could wrongly conclude any v1 peer is compatible.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does NOT promise that another instance speaks the same contract")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("read the peer's own manifest")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("parse error rather than as a clear refusal")))
            // And it must say it carries no data.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("carries no data")));
    }

    @Test
    void aRecognisedContractVersionShouldBeReadable() throws Exception {
        mockMvc.perform(get("/api/federation/compatibility")
                .queryParam("peerContractVersion", "v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readable").value(true))
            .andExpect(jsonPath("$.localContractVersion").value("v1"))
            .andExpect(jsonPath("$.explanation").value(
                containsString("can read contract v1")));
    }

    @Test
    void anUnrecognisedContractVersionShouldBeRefusedRatherThanGuessed() throws Exception {
        mockMvc.perform(get("/api/federation/compatibility")
                .queryParam("peerContractVersion", "v99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readable").value(false))
            // A best-effort parse of an unrecognised shape would produce data that looks valid and is not.
            .andExpect(jsonPath("$.explanation").value(
                containsString("Refusing rather than guessing")))
            .andExpect(jsonPath("$.explanation").value(
                containsString("looks valid and is not")));
    }

    @Test
    void aBlankContractVersionShouldBeRefused() throws Exception {
        mockMvc.perform(get("/api/federation/compatibility")
                .queryParam("peerContractVersion", "   "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readable").value(false));
    }

    @Test
    void theManifestShouldListEveryServedDatasetWithItsScope() throws Exception {
        mockMvc.perform(get("/api/federation/manifest"))
            .andExpect(status().isOk())
            // A consumer needs the scope to know which token to ask for.
            .andExpect(jsonPath("$.datasets[?(@.exportType=='SIGNALS')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_SIGNALS")))
            .andExpect(jsonPath("$.datasets[?(@.exportType=='PROPOSALS')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_PROPOSALS")))
            .andExpect(jsonPath("$.datasets[?(@.exportType=='VOTES')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_VOTES")))
            .andExpect(jsonPath("$.datasets[?(@.exportType=='DECISIONS')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_DECISIONS")))
            .andExpect(jsonPath("$.datasets[?(@.exportType=='METRICS')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_METRICS")))
            .andExpect(jsonPath("$.datasets[?(@.exportType=='PRIORITIZED_BACKLOG')].scope").value(
                org.hamcrest.Matchers.contains("EXPORT_PRIORITIZED_BACKLOG")));
    }

    @Test
    void theManifestShouldAddressCitiesByAStableKeyAndWithholdTheOnesThatOptedOut() throws Exception {
        Community federating = community("Riverside District", CommunityOpenDataPolicy.AGGREGATED_PUBLIC);
        Community quiet = community("Quiet District", CommunityOpenDataPolicy.DISABLED);

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
            mockMvc.perform(get("/api/federation/manifest"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Listed, addressed by the stable key.
        assertTrue(
            findCity(json.get("cities"), federating.getFederationKey()) != null,
            "a community that enabled open data must be listed: " + json.get("cities"));
        // Counted, not named: this endpoint is unauthenticated, so naming a community that has not
        // enabled open data would publish its existence to anyone who asked.
        assertTrue(
            json.get("citiesNotAdvertised").asInt() >= 1,
            "an opted-out community must be counted: " + json);
        assertTrue(
            findCity(json.get("cities"), quiet.getFederationKey()) == null,
            "an opted-out community must not be named in an unauthenticated manifest: " + json);
    }

    private static com.fasterxml.jackson.databind.JsonNode findCity(
        com.fasterxml.jackson.databind.JsonNode cities,
        String federationKey
    ) {
        if (cities == null) {
            return null;
        }
        for (var city : cities) {
            if (federationKey.equals(city.path("federationKey").asText())) {
                return city;
            }
        }
        return null;
    }

    @Test
    void twoCitiesShouldGetDistinctKeysSoNeitherCanShadowTheOther() {
        Community first = community("Riverside District", CommunityOpenDataPolicy.AGGREGATED_PUBLIC);
        Community second = community("Northgate", CommunityOpenDataPolicy.AGGREGATED_PUBLIC);

        org.junit.jupiter.api.Assertions.assertNotEquals(
            first.getFederationKey(), second.getFederationKey(),
            "two federating cities must not share a key");
        org.junit.jupiter.api.Assertions.assertNotNull(first.getFederationKey());
        org.junit.jupiter.api.Assertions.assertEquals(24, first.getFederationKey().length());
    }

    @Test
    void aRenamedSlugShouldNotChangeTheKeyAPeerHolds() {
        Community city = community("Riverside District", CommunityOpenDataPolicy.AGGREGATED_PUBLIC);
        String key = city.getFederationKey();

        city.setSlug("riverside-renamed-" + System.nanoTime());
        communityRepository.save(city);

        org.junit.jupiter.api.Assertions.assertEquals(
            key, city.getFederationKey(),
            "a federation key that moves under a peer breaks it silently");
    }

    private Community community(String name, CommunityOpenDataPolicy policy) {
        Community community = new Community();
        community.setName(name);
        // Unique per call: this class is not transactional, so its in-memory database keeps rows
        // between methods and the slug column is unique.
        community.setSlug(name.toLowerCase(Locale.ROOT).replace(' ', '-') + "-" + slugCounter.incrementAndGet());
        community.setDescription("Federation namespace");
        community.setOpenDataPolicy(policy);
        return communityRepository.save(community);
    }

    private static final AtomicInteger slugCounter = new AtomicInteger();

    @Test
    void theManifestShouldNotExposeAnyData() throws Exception {
        String body = mockMvc.perform(get("/api/federation/manifest"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // The manifest describes capabilities, not records. Reading an export still needs a token.
        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("\"title\""),
            "the manifest should not carry signal records: " + body);
        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("\"priorityScore\""),
            "the manifest should not carry scores: " + body);
    }
}