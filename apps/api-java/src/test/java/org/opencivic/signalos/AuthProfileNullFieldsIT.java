package org.opencivic.signalos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The seeded profile has no civicRole or bio. /api/auth/me used Map.ofEntries, which rejects null
 * values, so every page load for those accounts answered 500 while the session itself was fine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthProfileNullFieldsIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin")
    void meReturnsProfileWithNullOptionalFields() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.civicRole").value(nullValue()))
                .andExpect(jsonPath("$.bio").value(nullValue()));
    }
}
