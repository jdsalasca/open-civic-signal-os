package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignalCreateContractIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SignalRepository signalRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername("reporter").isEmpty()) {
            User user = new User("reporter", "{noop}pw", "reporter@test.dev", "ROLE_CITIZEN");
            user.setEnabled(true);
            user.setVerified(true);
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = "reporter", roles = {"CITIZEN"})
    void createSignalShouldPersistLocationAndMultipleEvidenceUrls() throws Exception {
        String response = mockMvc.perform(post("/api/signals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title":"Bus stop flooding after rain",
                      "description":"Water accumulates at the south bus stop and forces people into traffic lanes during morning commute.",
                      "category":"mobility",
                      "urgency":4,
                      "impact":4,
                      "affectedPeople":80,
                      "locationLabel":"South gate bus stop, block B",
                      "evidenceUrls":[
                        "https://example.com/flood-1.jpg",
                        "https://example.com/flood-2.jpg"
                      ],
                      "latitude":4.711,
                      "longitude":-74.0721
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Bus stop flooding after rain"))
            .andExpect(jsonPath("$.locationLabel").value("South gate bus stop, block B"))
            .andExpect(jsonPath("$.evidenceUrls.length()").value(2))
            .andExpect(jsonPath("$.evidenceUrls[0]").value("https://example.com/flood-1.jpg"))
            .andExpect(jsonPath("$.imageUrl").value("https://example.com/flood-1.jpg"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String signalId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        var persisted = signalRepository.findById(java.util.UUID.fromString(signalId));
        assertTrue(persisted.isPresent());
        assertEquals("South gate bus stop, block B", persisted.get().getLocationLabel());
        assertEquals(2, persisted.get().getEvidenceUrls().size());
        assertEquals("https://example.com/flood-1.jpg", persisted.get().getImageUrl());
    }
}
