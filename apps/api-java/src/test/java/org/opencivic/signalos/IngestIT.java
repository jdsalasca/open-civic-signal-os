package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingest must never fail a whole upload because one row is malformed: every row gets an
 * outcome, rejected rows are never persisted, and a dry run writes nothing.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ingestit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IngestIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User importer = new User("ingest_coord", "encoded", "ingest@example.com", "ROLE_CITIZEN");
        importer.setVerified(true);
        importer.setEnabled(true);
        UUID userId = userRepository.save(importer).getId();

        User plain = new User("ingest_member", "encoded", "member@example.com", "ROLE_CITIZEN");
        plain.setVerified(true);
        plain.setEnabled(true);
        UUID memberId = userRepository.save(plain).getId();

        Community community = new Community();
        community.setName("Ingest District");
        community.setSlug("ingest-district");
        community.setDescription("Import validation");
        communityId = communityRepository.save(community).getId();

        addMembership(userId, CommunityRole.COORDINATOR);
        addMembership(memberId, CommunityRole.MEMBER);
    }

    private void addMembership(UUID userId, CommunityRole role) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(userId);
        membershipRepository.save(membership);
    }

    @Test
    void malformedRowsShouldBeReportedWithoutFailingTheUpload() throws Exception {
        String csv = """
            title,description,category,urgency,impact,affectedPeople
            "Streetlight out, main corridor","Main corridor lights have been out for three nights.",infrastructure,4,4,120
            ,,infrastructure,2,2,10
            Pothole on school route,Deep pothole in front of the primary school gate.,infrastructure,3,3,60
            Bad numbers row,This row has non-numeric urgency in the export.,infrastructure,high,3,60
            Out of range row,This row has urgency outside the allowed band.,infrastructure,9,3,60
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "reports.csv", csv, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("CSV_EXPORT"))
            .andExpect(jsonPath("$.committed").value(false))
            .andExpect(jsonPath("$.totalRows").value(5))
            .andExpect(jsonPath("$.acceptedRows").value(2))
            .andExpect(jsonPath("$.rejectedRows").value(3))
            .andExpect(jsonPath("$.contentSha256").isNotEmpty())
            .andExpect(jsonPath("$.detectedColumns", hasSize(6)))
            .andExpect(jsonPath("$.outcomes", hasSize(5)))
            // Row 3 is rejected for a blank title and a blank description.
            // Rows 5 and 6 are rejected once each: urgency not a number, then out of range.
            .andExpect(jsonPath("$.errors", hasSize(4)))
            .andExpect(jsonPath("$.errors[?(@.rowIndex==3 && @.field=='title')].code")
                .value(hasSize(1)))
            .andExpect(jsonPath("$.errors[?(@.rowIndex==3 && @.field=='description')].code")
                .value(hasSize(1)))
            .andExpect(jsonPath("$.errors[?(@.rowIndex==5 && @.code=='NOT_A_NUMBER')]").exists())
            .andExpect(jsonPath("$.errors[?(@.rowIndex==6 && @.code=='OUT_OF_RANGE')]").exists())
            // Accepted rows carry no errors at all.
            .andExpect(jsonPath("$.outcomes[?(@.rowIndex==2)].errors", hasSize(1)))
            .andExpect(jsonPath("$.outcomes[?(@.rowIndex==4)].errors", hasSize(1)));

        // A dry run must not write anything.
        org.junit.jupiter.api.Assertions.assertEquals(0, signalRepository.count());
    }

    @Test
    void quotedCommasAndEscapedQuotesShouldNotBreakParsing() throws Exception {
        String csv = """
            title,description,category,urgency,impact,affectedPeople
            "Lamp, main corridor","The lamp at the corner, near the school, is out.",infrastructure,4,4,90
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "quoted.csv", csv, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(1))
            .andExpect(jsonPath("$.acceptedRows").value(1))
            .andExpect(jsonPath("$.rejectedRows").value(0));
    }

    @Test
    void commitShouldPersistOnlyAcceptedRowsWithProvenance() throws Exception {
        String csv = """
            title,description,category,urgency,impact,affectedPeople
            "Streetlight out","Main corridor lights have been out for three nights.",infrastructure,4,4,120
            ,,,infrastructure,2,2,10
            """;

        mockMvc.perform(post("/api/ingest/commit")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "reports.csv", csv, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.committed").value(true))
            .andExpect(jsonPath("$.acceptedRows").value(1))
            .andExpect(jsonPath("$.rejectedRows").value(1))
            .andExpect(jsonPath("$.outcomes[0].signalId").isNotEmpty())
            .andExpect(jsonPath("$.outcomes[1].signalId").doesNotExist())
            .andExpect(jsonPath("$.outcomes[0].sourceRef").value("reports.csv#2"));

        org.junit.jupiter.api.Assertions.assertEquals(1, signalRepository.count());
        signalRepository.findAll().forEach(signal -> {
            org.junit.jupiter.api.Assertions.assertEquals("CSV_IMPORT", signal.getSourceChannel().name());
            org.junit.jupiter.api.Assertions.assertEquals("reports.csv#2", signal.getSourceRef());
            org.junit.jupiter.api.Assertions.assertNotNull(signal.getTransformationVersion());
        });
    }

    @Test
    void telegramJsonExportShouldValidateAndCiteTheSender() throws Exception {
        String json = """
            [
              {"date": "2026-03-24T08:10:00", "from": "vecina", "text": "There is no water on Calle 12 since yesterday morning."},
              {"date": "2026-03-24T08:14:00", "from": "vecino", "text": "ok"}
            ]
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("TELEGRAM_EXPORT", "chat_export.json", json, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("TELEGRAM_EXPORT"))
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.acceptedRows").value(1))
            .andExpect(jsonPath("$.rejectedRows").value(1))
            .andExpect(jsonPath("$.outcomes[0].sourceRef").value("chat_export.json#1"));
    }

    @Test
    void whatsappCsvExportShouldValidate() throws Exception {
        String csv = """
            date,sender,message
            2026-03-24 08:10,+34 600 000 000,"The park lighting has been broken since the storm last week."
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("WHATSAPP_EXPORT", "whatsapp.csv", csv, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(1))
            .andExpect(jsonPath("$.acceptedRows").value(1));
    }

    @Test
    void chatExportWithoutARecognisedTextColumnShouldFailLoudly() throws Exception {
        String csv = """
            date,sender,note
            2026-03-24 08:10,vecina,hello
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("WHATSAPP_EXPORT", "whatsapp.csv", csv, false)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void malformedChatJsonShouldFailLoudly() throws Exception {
        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("TELEGRAM_EXPORT", "chat.json", "{ not json", false)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void memberShouldNotBeAbleToIngest() throws Exception {
        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "reports.csv", "title,description\na,b", false)))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownSourceShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CARRIER_PIGEON", "x.csv", "title,description\na,b", false)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unterminatedQuoteShouldBeRejectedRatherThanMisparsed() throws Exception {
        String csv = "title,description\n\"unclosed,Some text here\n";

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "broken.csv", csv, false)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rowWithTheWrongColumnCountShouldBeRejectedRatherThanShifted() throws Exception {
        // One extra value here would silently push every later column into the wrong field,
        // landing "infrastructure" under urgency. The row must be rejected, not coerced.
        String csv = """
            title,description,category,urgency,impact,affectedPeople
            Valid report row,This row lines up with the header exactly.,infrastructure,3,3,40
            Short row,This row is missing its trailing values
            """;

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "ragged.csv", csv, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(2))
            .andExpect(jsonPath("$.acceptedRows").value(1))
            .andExpect(jsonPath("$.rejectedRows").value(1))
            .andExpect(jsonPath("$.errors[?(@.rowIndex==3 && @.code=='COLUMN_COUNT_MISMATCH')]").exists());
    }

    @Test
    void oversizedExportShouldBeRejectedBeforeParsing() throws Exception {
        StringBuilder csv = new StringBuilder("title,description\n");
        for (int i = 0; i < 2100; i++) {
            csv.append("Row ").append(i).append(",A description long enough to pass validation here.\n");
        }

        mockMvc.perform(post("/api/ingest/validate")
                .with(user("ingest_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request("CSV_EXPORT", "big.csv", csv.toString(), false)))
            .andExpect(status().isConflict());
    }

    private String request(String source, String fileName, String content, boolean commit) {
        return """
            {
              "communityId": "%s",
              "source": "%s",
              "fileName": "%s",
              "content": %s,
              "commit": %s
            }
            """.formatted(communityId, source, fileName, jsonString(content), commit);
    }

    private String jsonString(String value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}