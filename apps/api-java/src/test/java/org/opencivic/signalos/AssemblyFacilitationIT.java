package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A facilitator needs a plan to pace against, and the platform must not pretend to run the meeting.
 *
 * <p>Overrunning is reported, not prevented: a facilitator who gives an item twice its slot may be
 * making the right call about the room, and the platform has no standing to call it wrong.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:facilitation;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssemblyFacilitationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("facil_coord", "encoded", "facil@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Facilitation");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void settingAnAgendaShouldRecordTheRunningOrder() throws Exception {
        String assemblyId = createAssemblyId();

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        {"title": "Welcome and ground rules", "plannedMinutes": 5},
                        {"title": "Water main repair", "plannedMinutes": 20, "subjectType": "PROPOSAL"},
                        {"title": "Streetlight replacement", "plannedMinutes": 15},
                        {"title": "Close and next steps", "plannedMinutes": 5}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemCount").value(4))
            .andExpect(jsonPath("$.totalPlannedMinutes").value(45))
            .andExpect(jsonPath("$.agenda", hasSize(4)))
            .andExpect(jsonPath("$.agenda[0].position").value(1))
            .andExpect(jsonPath("$.agenda[0].title").value("Welcome and ground rules"))
            .andExpect(jsonPath("$.agenda[1].title").value("Water main repair"))
            // The cumulative time to reach each item, so a facilitator can see where they should be.
            .andExpect(jsonPath("$.runningOrder[1].cumulativePlannedMinutes").value(25))
            .andExpect(jsonPath("$.runningOrder[3].cumulativePlannedMinutes").value(45));
    }

    @Test
    void theFacilitationShouldSayItDoesNotRunTheMeeting() throws Exception {
        String assemblyId = createAssemblyId();
        setAgenda(assemblyId);

        mockMvc.perform(get("/api/community/assemblies/{id}/facilitation", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does NOT run the meeting")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("judgement about the room")))
            // And it must not reorder by score.
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does not reorder an agenda by score")));
    }

    @Test
    void aClosedAssemblyShouldNotAcceptAnAgendaChange() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);
        closeAssembly(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [{"title": "Late addition", "plannedMinutes": 10}]}
                    """))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("change what the record says was planned")));
    }

    @Test
    void settingAnAgendaAgainShouldReplaceItRatherThanAppend() throws Exception {
        String assemblyId = createAssemblyId();
        setAgenda(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [{"title": "Only item", "plannedMinutes": 10}]}
                    """))
            .andExpect(status().isOk())
            // Replacing, so a facilitator editing mid-meeting cannot end up with two items at the
            // same position.
            .andExpect(jsonPath("$.itemCount").value(1))
            .andExpect(jsonPath("$.agenda[0].title").value("Only item"));
    }

    @Test
    void anUntitledItemShouldBeRejected() throws Exception {
        String assemblyId = createAssemblyId();

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [{"title": "   ", "plannedMinutes": 10}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("needs a title")));
    }

    @Test
    void anAbsurdItemDurationShouldBeRejected() throws Exception {
        String assemblyId = createAssemblyId();

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [{"title": "Marathon item", "plannedMinutes": 5000}]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void tooManyItemsShouldBeRejectedRatherThanTruncated() throws Exception {
        String assemblyId = createAssemblyId();
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            if (i > 0) {
                items.append(",");
            }
            items.append("{\"title\": \"Item ").append(i).append("\", \"plannedMinutes\": 5}");
        }

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\": [" + items + "]}"))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("At most 50 agenda items")));
    }

    @Test
    void pacingShouldBeNotStartedBeforeTheAssemblyOpens() throws Exception {
        String assemblyId = createAssemblyId();
        setAgenda(assemblyId);

        mockMvc.perform(get("/api/community/assemblies/{id}/facilitation", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pacing").value("NOT_STARTED"))
            .andExpect(jsonPath("$.elapsedMinutes").doesNotExist())
            .andExpect(jsonPath("$.interpretation").value(
                containsString("not open, so no elapsed time is being tracked")));
    }

    @Test
    void pacingShouldBeOnPlanOnceOpened() throws Exception {
        String assemblyId = createAssemblyId();
        setAgenda(assemblyId);
        openAssembly(assemblyId);

        mockMvc.perform(get("/api/community/assemblies/{id}/facilitation", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            // Just opened, so elapsed is 0 against a 45 minute plan.
            .andExpect(jsonPath("$.pacing").value("ON_PLAN"))
            .andExpect(jsonPath("$.elapsedMinutes").value(0))
            .andExpect(jsonPath("$.totalPlannedMinutes").value(45));
    }

    @Test
    void anAssemblyWithNoAgendaShouldSayThereIsNothingToPaceAgainst() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);

        mockMvc.perform(get("/api/community/assemblies/{id}/facilitation", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemCount").value(0))
            .andExpect(jsonPath("$.pacing").value("NO_PLAN"))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("nothing to pace against")));
    }

    @Test
    void aResidentWithoutTheScopeShouldNotSetAnAgenda() throws Exception {
        String assemblyId = createAssemblyId();

        User resident = new User("facil_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_resident").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"items": [{"title": "Not mine", "plannedMinutes": 10}]}
                    """))
            .andExpect(status().isForbidden());
    }

    private String createAssemblyId() throws Exception {
        String created = mockMvc.perform(post("/api/community/assemblies")
                .with(user("facil_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "title": "April townhall",
                      "scheduledFor": "2026-04-10T18:00:00",
                      "location": "Community hall"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return created.replaceAll("(?s).*\"assemblyId\":\"([^\"]+)\".*", "$1");
    }

    private void setAgenda(String assemblyId) throws Exception {
        mockMvc.perform(post("/api/community/assemblies/{id}/agenda", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "items": [
                        {"title": "Welcome", "plannedMinutes": 5},
                        {"title": "Water main", "plannedMinutes": 20},
                        {"title": "Streetlight", "plannedMinutes": 15},
                        {"title": "Close", "plannedMinutes": 5}
                      ]
                    }
                    """))
            .andExpect(status().isOk());
    }

    private void openAssembly(String assemblyId) throws Exception {
        mockMvc.perform(post("/api/community/assemblies/{id}/open", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());
    }

    private void closeAssembly(String assemblyId) throws Exception {
        mockMvc.perform(post("/api/community/assemblies/{id}/close", assemblyId)
                .with(user("facil_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"minutes": "Closed."}
                    """))
            .andExpect(status().isOk());
    }
}