package org.opencivic.signalos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalSourceChannel;
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
 * A municipal export lands in a city procurement system that this platform does not control.
 * Two failure modes matter most: a report silently routed to the wrong department, and a
 * resident's contact details leaving with it.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:municipalit;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MunicipalTicketExportIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("muni_coord", "encoded", "muni@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Municipal handover");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void fieldMapShouldBeReadableWithoutCallingTheExport() throws Exception {
        mockMvc.perform(get("/api/community/exports/municipal-tickets/field-map")
                .with(user("muni_coord").roles("CITIZEN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(11)))
            .andExpect(jsonPath("$[0].name").value("ticket_ref"))
            .andExpect(jsonPath("$[1].name").value("external_category_code"))
            // A municipality should not mistake our score for their own priority field.
            .andExpect(jsonPath("$[?(@.name=='priority_score')].description")
                .value(org.hamcrest.Matchers.hasItem(
                    org.hamcrest.Matchers.containsString("NOT the city's own priority"))));
    }

    @Test
    void mappedCategoriesShouldExportWithTheCityServiceCode() throws Exception {
        signal("Pothole on school route", "infrastructure", "Main corridor", 82.0);

        mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "categoryMap": {"infrastructure": "SRV-ROADS-01"}
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exportedCount").value(1))
            .andExpect(jsonPath("$.excludedCount").value(0))
            .andExpect(jsonPath("$.unmappedCategories", hasSize(0)))
            .andExpect(jsonPath("$.tickets[0].externalCategoryCode").value("SRV-ROADS-01"))
            .andExpect(jsonPath("$.tickets[0].ticketRef").value(
                org.hamcrest.Matchers.startsWith("OCS-")))
            .andExpect(jsonPath("$.tickets[0].platformUrl").value(
                org.hamcrest.Matchers.containsString("/signals/")))
            // The field map travels with the export.
            .andExpect(jsonPath("$.fieldMap", hasSize(11)));
    }

    @Test
    void unmappedCategoryShouldBeExcludedAndReportedRatherThanCoerced() throws Exception {
        signal("Pothole on school route", "infrastructure", "Main corridor", 82.0);
        signal("Broken water valve", "utilities", "Calle 12", 95.0);

        mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "categoryMap": {"infrastructure": "SRV-ROADS-01"}
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exportedCount").value(1))
            .andExpect(jsonPath("$.excludedCount").value(1))
            .andExpect(jsonPath("$.unmappedCategories", hasSize(1)))
            .andExpect(jsonPath("$.unmappedCategories[0].category").value("utilities"))
            .andExpect(jsonPath("$.unmappedCategories[0].affectedReports").value(1))
            .andExpect(jsonPath("$.unmappedCategories[0].guidance").value(
                org.hamcrest.Matchers.containsString("categoryMap")));
    }

    @Test
    void unmappedCategoriesShouldBeOptInAndClearlyPrefixed() throws Exception {
        signal("Broken water valve", "utilities", "Calle 12", 95.0);

        mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "includeUnmapped": true
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exportedCount").value(1))
            .andExpect(jsonPath("$.excludedCount").value(0))
            .andExpect(jsonPath("$.tickets[0].externalCategoryCode").value("UNMAPPED.UTILITIES"))
            // Still reported, so the city can see what it is not routing anywhere.
            .andExpect(jsonPath("$.unmappedCategories", hasSize(1)));
    }

    @Test
    void contactDetailsShouldNotLeaveThePlatform() throws Exception {
        signal(
            "Water outage, call maria.lopez@example.org",
            "utilities",
            "Calle 12, ring +34 600 11 22 33",
            95.0);

        String body = mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "includeUnmapped": true
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("maria.lopez@example.org"), "email leaked into a municipal export: " + body);
        org.junit.jupiter.api.Assertions.assertFalse(
            body.contains("34600112233"), "phone leaked into a municipal export: " + body);
        org.junit.jupiter.api.Assertions.assertTrue(
            body.contains("[email redacted]") && body.contains("[phone redacted]"),
            "expected visible redaction markers: " + body);
    }

    @Test
    void ticketReferencesShouldBeStableAcrossRegenerations() throws Exception {
        signal("Pothole on school route", "infrastructure", "Main corridor", 82.0);

        String first = exportWithMap();
        String second = exportWithMap();

        org.junit.jupiter.api.Assertions.assertEquals(
            first.replaceAll("\"generatedAt\":\"[^\"]+\"", ""),
            second.replaceAll("\"generatedAt\":\"[^\"]+\"", ""),
            "regenerating a municipal export must not invent new ticket references");
    }

    @Test
    void importProvenanceShouldSurviveTheHandover() throws Exception {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle("Bulk imported streetlight failure");
        signal.setDescription("Imported from the neighbourhood association spreadsheet.");
        signal.setCategory("infrastructure");
        signal.setStatus("OPEN");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(90);
        signal.setCommunityVotes(3);
        signal.setPriorityScore(70.0);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 90, 3));
        signal.setLocationLabel("Avenida Central");
        signal.setSourceChannel(SignalSourceChannel.CSV_IMPORT);
        signal.setSourceRef("reports.csv#42");
        signal.setTransformationVersion("v1");
        signal.setCreatedAt(LocalDateTime.parse("2026-03-04T09:00:00"));
        signalRepository.save(signal);

        mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "categoryMap": {"infrastructure": "SRV-ROADS-01"}
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tickets[0].sourceChannel").value("CSV_IMPORT"))
            .andExpect(jsonPath("$.tickets[0].sourceRef").value("reports.csv#42"));
    }

    @Test
    void memberWithoutTheExportScopeShouldNotHandOverDataToACity() throws Exception {
        User member = new User("muni_member", "encoded", "member@example.com", "ROLE_CITIZEN");
        member.setVerified(true);
        member.setEnabled(true);
        UUID memberId = userRepository.save(member).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(memberId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_member").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "includeUnmapped": true}
                    """.formatted(communityId)))
            .andExpect(status().isForbidden());
    }

    private String exportWithMap() throws Exception {
        return mockMvc.perform(post("/api/community/exports/municipal-tickets")
                .with(user("muni_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "categoryMap": {"infrastructure": "SRV-ROADS-01"}
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private UUID signal(String title, String category, String location, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Reported by a resident for the municipal handover test.");
        signal.setCategory(category);
        signal.setStatus("OPEN");
        signal.setUrgency(3);
        signal.setImpact(4);
        signal.setAffectedPeople(80);
        signal.setCommunityVotes(6);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(3, 4, 80, 6));
        signal.setLocationLabel(location);
        signal.setCreatedAt(LocalDateTime.parse("2026-03-04T09:00:00"));
        return signalRepository.save(signal).getId();
    }
}