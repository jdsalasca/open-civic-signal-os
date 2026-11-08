package org.opencivic.signalos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.opencivic.signalos.repository.CommunityRepository;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.auth.require-email-verification=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityPlaceRegistrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityRepository communityRepository;

    @Test
    void registeringAPlaceNormalizesItAndRejectsTheSamePlaceUnderAnotherSpelling() throws Exception {
        register(
            "place_owner_1", "place1@example.com",
            """
                "communityCity":"Bogotá","communityLocality":"Chapinero",
                "communityNeighborhood":"El Chicó","communityCountry":"Colombia",
                "communityRegion":"Cundinamarca"
                """
        ).andExpect(status().isOk());

        var saved = communityRepository.findByPlaceKey("bogota|chapinero|el-chico").orElseThrow();
        assertEquals("Bogotá", saved.getCity());
        assertEquals("Chapinero", saved.getLocality());
        assertEquals("El Chicó", saved.getNeighborhood());
        assertEquals("El Chicó", saved.getName());

        var city = communityRepository.findByPlaceKey("bogota||").orElseThrow();
        assertEquals("Bogotá", city.getName());
        assertEquals(city.getId(), saved.getParentCommunityId());

        register(
            "place_owner_2", "place2@example.com",
            """
                "communityCity":"BOGOTA","communityLocality":"chapinero",
                "communityNeighborhood":"el chico","communityCountry":"Colombia"
                """
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("already exists for this place")));
    }

    @Test
    void aDifferentNeighborhoodInTheSameLocalityIsAnotherCommunity() throws Exception {
        register(
            "place_owner_3", "place3@example.com",
            """
                "communityCity":"Medellín","communityLocality":"Belén",
                "communityNeighborhood":"La Mota","communityCountry":"Colombia"
                """
        ).andExpect(status().isOk());

        register(
            "place_owner_4", "place4@example.com",
            """
                "communityCity":"Medellín","communityLocality":"Belén",
                "communityNeighborhood":"La Cabrera","communityCountry":"Colombia"
                """
        ).andExpect(status().isOk());
    }

    @Test
    void aLocalityWithoutACityIsRejected() throws Exception {
        register(
            "place_owner_6", "place6@example.com",
            """
                "communityLocality":"Chapinero","communityNeighborhood":"El Chicó",
                "communityCountry":"Colombia"
                """
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("City is required")));
    }

    @Test
    void unsupportedCharactersAreRejectedBeforeTheCommunityIsCreated() throws Exception {
        register(
            "place_owner_5", "place5@example.com",
            """
                "communityCity":"Bogotá<script>","communityLocality":"Chapinero"
                """
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("unsupported characters")));
    }

    private org.springframework.test.web.servlet.ResultActions register(
        String username,
        String email,
        String placeFields
    ) throws Exception {
        String body = """
            {
              "username":"%s",
              "password":"Passw0rd!",
              "email":"%s",
              %s
            }
            """.formatted(username, email, placeFields);
        return mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }
}
