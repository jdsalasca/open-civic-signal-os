package org.opencivic.signalos;

import static org.hamcrest.Matchers.containsString;
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
 * An assembly record is not the official minutes, and a decision made without captured evidence is
 * not reviewable. Both are stated on every read rather than left for a reader to assume.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:assembly;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityAssemblyIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;

    private UUID communityId;
    private UUID coordinatorId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("assembly_coord", "encoded", "assembly@example.com", "ROLE_CITIZEN");
        coordinator.setVerified(true);
        coordinator.setEnabled(true);
        coordinatorId = userRepository.save(coordinator).getId();

        Community community = new Community();
        community.setName("Riverside District");
        community.setSlug("riverside-district");
        community.setDescription("Assembly mode");
        communityId = communityRepository.save(community).getId();

        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(coordinatorId);
        membership.setRole(CommunityRole.COORDINATOR);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);
    }

    @Test
    void creatingAnAssemblyShouldRecordItWithoutEvidence() throws Exception {
        String created = createAssembly();

        org.junit.jupiter.api.Assertions.assertTrue(created.contains("\"status\":\"SCHEDULED\""));
        org.junit.jupiter.api.Assertions.assertTrue(created.contains("\"evidenceCaptured\":false"));
        // The gap is reported rather than hidden.
        org.junit.jupiter.api.Assertions.assertTrue(
            created.contains("cannot be reproduced"),
            "expected the missing-evidence warning, got: " + created);
    }

    @Test
    void attachingEvidenceShouldRecordTheSnapshotHash() throws Exception {
        String assemblyId = createAssemblyId();
        String snapshotId = createSnapshot();

        mockMvc.perform(post("/api/community/assemblies/{id}/evidence", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("snapshotId", snapshotId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evidenceCaptured").value(true))
            .andExpect(jsonPath("$.snapshotId").value(snapshotId))
            // The hash is what makes the ranking checkable afterwards.
            .andExpect(jsonPath("$.snapshotContentHash").isNotEmpty())
            .andExpect(jsonPath("$.interpretation").value(
                containsString("frozen and its content hash is recorded")));
    }

    @Test
    void theRecordShouldSayItIsNotTheOfficialMinutes() throws Exception {
        String assemblyId = createAssemblyId();

        mockMvc.perform(get("/api/community/assemblies/{id}", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interpretation").value(
                containsString("NOT the official minutes")))
            .andExpect(jsonPath("$.interpretation").value(
                containsString("does not make them")));
    }

    @Test
    void recordingADecisionShouldRequireARationale() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/decisions", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"decision": "APPROVED", "rationale": "   "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("rationale is required")));
    }

    @Test
    void aRecordedDecisionShouldAppearInTheAssembly() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/decisions", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "decision": "APPROVED",
                      "rationale": "The water main affects the most residents and the cost is within the envelope.",
                      "subjectType": "PROPOSAL"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decisions", hasSize(1)))
            .andExpect(jsonPath("$.decisions[0].decision").value("APPROVED"))
            .andExpect(jsonPath("$.decisions[0].rationale").value(
                containsString("affects the most residents")));
    }

    @Test
    void aDecisionAfterClosingShouldBeRefused() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);
        closeAssembly(assemblyId);

        // A decision recorded afterwards would appear in the record as having been made in the room.
        mockMvc.perform(post("/api/community/assemblies/{id}/decisions", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"decision": "APPROVED", "rationale": "Decided later."}
                    """))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("appear in the record as having been made in the room")));
    }

    @Test
    void evidenceAfterClosingShouldBeRefused() throws Exception {
        String assemblyId = createAssemblyId();
        String snapshotId = createSnapshot();
        openAssembly(assemblyId);
        closeAssembly(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/evidence", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("snapshotId", snapshotId))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("change what the record says was deliberated over")));
    }

    @Test
    void closingTwiceShouldBeRefused() throws Exception {
        String assemblyId = createAssemblyId();
        openAssembly(assemblyId);
        closeAssembly(assemblyId);

        mockMvc.perform(post("/api/community/assemblies/{id}/close", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"minutes": "Second close."}
                    """))
            .andExpect(status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("erase the record of when it ended")));
    }

    @Test
    void anAssemblyWithoutATitleOrDateShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/community/assemblies")
                .with(user("assembly_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "title": "  ", "scheduledFor": "2026-04-10T18:00:00"}
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("title is required")));

        mockMvc.perform(post("/api/community/assemblies")
                .with(user("assembly_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "title": "Budget assembly"}
                    """.formatted(communityId)))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                .content().string(containsString("scheduled time is required")));
    }

    @Test
    void aSnapshotFromAnotherCommunityShouldNotBeAttachable() throws Exception {
        String assemblyId = createAssemblyId();

        Community other = new Community();
        other.setName("Elsewhere");
        other.setSlug("elsewhere");
        other.setDescription("Other");
        UUID otherId = communityRepository.save(other).getId();

        mockMvc.perform(post("/api/community/assemblies/{id}/evidence", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .queryParam("snapshotId", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void aResidentWithoutTheScopeShouldNotConveneOrRead() throws Exception {
        User resident = new User("assembly_resident", "encoded", "res@example.com", "ROLE_CITIZEN");
        resident.setVerified(true);
        resident.setEnabled(true);
        UUID residentId = userRepository.save(resident).getId();
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(residentId);
        membership.setRole(CommunityRole.MEMBER);
        membership.setCreatedBy(coordinatorId);
        membershipRepository.save(membership);

        mockMvc.perform(post("/api/community/assemblies")
                .with(user("assembly_resident").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "title": "Not mine", "scheduledFor": "2026-04-10T18:00:00"}
                    """.formatted(communityId)))
            .andExpect(status().isForbidden());
    }

    private String createAssembly() throws Exception {
        return mockMvc.perform(post("/api/community/assemblies")
                .with(user("assembly_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "communityId": "%s",
                      "title": "April budget assembly",
                      "scheduledFor": "2026-04-10T18:00:00",
                      "location": "Community hall"
                    }
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String createAssemblyId() throws Exception {
        return createAssembly().replaceAll("(?s).*\"assemblyId\":\"([^\"]+)\".*", "$1");
    }

    private String createSnapshot() throws Exception {
        signal("Water main break", "utilities", 313.0);
        String created = mockMvc.perform(post("/api/community/explainability-snapshots")
                .with(user("assembly_coord").roles("CITIZEN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"communityId": "%s", "label": "April assembly evidence"}
                    """.formatted(communityId)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return created.replaceAll("(?s).*\"snapshotId\":\"([^\"]+)\".*", "$1");
    }

    private void openAssembly(String assemblyId) throws Exception {
        mockMvc.perform(post("/api/community/assemblies/{id}/open", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString()))
            .andExpect(status().isOk());
    }

    private void closeAssembly(String assemblyId) throws Exception {
        mockMvc.perform(post("/api/community/assemblies/{id}/close", assemblyId)
                .with(user("assembly_coord").roles("CITIZEN"))
                .queryParam("communityId", communityId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"minutes": "Twelve residents attended. Two decisions recorded."}
                    """))
            .andExpect(status().isOk());
    }

    private void signal(String title, String category, double score) {
        Signal signal = new Signal();
        signal.setId(UUID.randomUUID());
        signal.setCommunityId(communityId);
        signal.setAuthorId(coordinatorId);
        signal.setTitle(title);
        signal.setDescription("Recorded for the assembly test.");
        signal.setCategory(category);
        signal.setStatus("NEW");
        signal.setUrgency(4);
        signal.setImpact(4);
        signal.setAffectedPeople(120);
        signal.setCommunityVotes(8);
        signal.setPriorityScore(score);
        signal.setScoreBreakdown(new ScoreBreakdown(4, 4, 120, 8));
        signal.setLocationLabel("Riverside");
        signal.setCreatedAt(LocalDateTime.now().minusDays(5));
        signalRepository.save(signal);
    }
}