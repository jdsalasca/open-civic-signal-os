package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityDecisionBasisType;
import org.opencivic.signalos.domain.CommunityDecisionStatus;
import org.opencivic.signalos.domain.CommunityDecisionType;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityProjectStatus;
import org.opencivic.signalos.domain.CommunityProjectTask;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.ScoreBreakdown;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalStatusEntry;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityProjectTaskRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.SignalStatusEntryRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommunityTrustMetricsIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CommunityMembershipRepository membershipRepository;
    @Autowired private SignalRepository signalRepository;
    @Autowired private SignalStatusEntryRepository signalStatusEntryRepository;
    @Autowired private CommunityProposalRepository proposalRepository;
    @Autowired private CommunityDecisionRepository decisionRepository;
    @Autowired private CommunityProjectBoardRepository boardRepository;
    @Autowired private CommunityProjectTaskRepository taskRepository;

    private UUID communityId;

    @BeforeEach
    void setUp() {
        User coordinator = new User("trust_coord", "{noop}pw", "trust-coord@test.dev", "ROLE_CITIZEN");
        coordinator.setEnabled(true);
        coordinator.setVerified(true);
        coordinator = userRepository.save(coordinator);

        User member = new User("trust_member", "{noop}pw", "trust-member@test.dev", "ROLE_CITIZEN");
        member.setEnabled(true);
        member.setVerified(true);
        member = userRepository.save(member);

        User resident = new User("trust_resident", "{noop}pw", "trust-resident@test.dev", "ROLE_CITIZEN");
        resident.setEnabled(true);
        resident.setVerified(true);
        resident = userRepository.save(resident);

        Community community = new Community();
        community.setName("Trust District");
        community.setSlug("trust-district");
        community.setDescription("Metrics scope");
        community = communityRepository.save(community);
        communityId = community.getId();

        membershipRepository.save(createMembership(communityId, coordinator.getId(), CommunityRole.COORDINATOR, coordinator.getId()));
        membershipRepository.save(createMembership(communityId, member.getId(), CommunityRole.MEMBER, coordinator.getId()));
        membershipRepository.save(createMembership(communityId, resident.getId(), CommunityRole.MEMBER, coordinator.getId()));

        Signal resolvedSignal = new Signal(
            UUID.randomUUID(),
            "Repair the crossing lights",
            "Lights fail every rainy afternoon.",
            "SAFETY",
            5,
            5,
            12,
            4,
            180,
            new ScoreBreakdown(5, 5, 12, 4),
            "RESOLVED",
            List.of(),
            coordinator.getId(),
            LocalDateTime.now().minusDays(3),
            communityId
        );
        resolvedSignal = signalRepository.save(resolvedSignal);

        Signal openSignal = new Signal(
            UUID.randomUUID(),
            "Water leak near the sports court",
            "The leak is growing after each training night.",
            "INFRASTRUCTURE",
            4,
            4,
            20,
            2,
            140,
            new ScoreBreakdown(4, 4, 20, 2),
            "NEW",
            List.of(),
            resident.getId(),
            LocalDateTime.now().minusDays(2),
            communityId
        );
        openSignal = signalRepository.save(openSignal);

        signalStatusEntryRepository.save(new SignalStatusEntry(
            resolvedSignal.getId(),
            "IN_PROGRESS",
            "RESOLVED",
            "STATUS_CHANGED",
            "trust_coord",
            "Repaired by the municipal electrical crew.",
            null
        ));

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(communityId);
        proposal.setAuthorId(coordinator.getId());
        proposal.setRelatedSignalId(resolvedSignal.getId());
        proposal.setTitle("Approve crossing-light repair follow-through");
        proposal.setProblemStatement("The crossing lights keep failing during school dismissal.");
        proposal.setProposedSolution("Approve a recurring inspection and replacement plan.");
        proposal.setEstimatedCost("COP 4M");
        proposal.setBeneficiariesSummary("Students and families crossing after school.");
        proposal.setSupportingLinks(List.of("https://example.com/trust/proposal"));
        proposal.setCreatedAt(LocalDateTime.now().minusDays(2));
        proposal.setUpdatedAt(LocalDateTime.now().minusDays(1));
        proposal = proposalRepository.save(proposal);

        CommunityDecision decision = new CommunityDecision();
        decision.setCommunityId(communityId);
        decision.setLinkedProposalId(proposal.getId());
        decision.setDecidedBy(coordinator.getId());
        decision.setExecutionOwnerId(member.getId());
        decision.setDecisionType(CommunityDecisionType.APPROVAL);
        decision.setDecisionStatus(CommunityDecisionStatus.IN_EXECUTION);
        decision.setApprovalBasisType(CommunityDecisionBasisType.COORDINATOR_REVIEW);
        decision.setTitle("Record recurring inspection decision");
        decision.setSummary("The community recorded the inspection commitment and execution owner.");
        decision.setApprovalBasisSummary("Coordinator review and resident evidence were enough for this execution decision.");
        decision.setDecidedAt(LocalDateTime.now().minusDays(1));
        decision.setEffectiveDate(LocalDate.now().plusDays(1));
        decision.setCreatedAt(LocalDateTime.now().minusDays(1));
        decision.setUpdatedAt(LocalDateTime.now().minusDays(1));
        decisionRepository.save(decision);

        CommunityProjectBoard board = new CommunityProjectBoard();
        board.setCommunityId(communityId);
        board.setLinkedProposalId(proposal.getId());
        board.setOwnerId(member.getId());
        board.setTitle("Crossing lights execution board");
        board.setSummary("Track inspection scheduling and light replacement.");
        board.setCreatedAt(LocalDateTime.now().minusDays(1));
        board.setUpdatedAt(LocalDateTime.now().minusHours(12));
        board = boardRepository.save(board);

        CommunityProjectTask doneTask = new CommunityProjectTask();
        doneTask.setProjectBoardId(board.getId());
        doneTask.setTitle("Confirm repair crew visit");
        doneTask.setDetails("Crew visit confirmed and logged.");
        doneTask.setStatus(CommunityProjectStatus.DONE);
        doneTask.setAssigneeId(member.getId());
        doneTask.setDueDate(LocalDate.now().plusDays(1));
        doneTask.setCreatedAt(LocalDateTime.now().minusDays(1));
        doneTask.setUpdatedAt(LocalDateTime.now().minusHours(5));
        taskRepository.save(doneTask);

        CommunityProjectTask activeTask = new CommunityProjectTask();
        activeTask.setProjectBoardId(board.getId());
        activeTask.setTitle("Publish repair memo");
        activeTask.setDetails("Publish the inspection memo for residents.");
        activeTask.setStatus(CommunityProjectStatus.IN_PROGRESS);
        activeTask.setAssigneeId(coordinator.getId());
        activeTask.setDueDate(LocalDate.now().plusDays(2));
        activeTask.setCreatedAt(LocalDateTime.now().minusDays(1));
        activeTask.setUpdatedAt(LocalDateTime.now().minusHours(2));
        taskRepository.save(activeTask);
    }

    @Test
    @WithMockUser(username = "trust_coord", roles = {"CITIZEN"})
    void returnsDeterministicTrustMetricsForCommunityAndPeriod() throws Exception {
        mockMvc.perform(get("/api/community/trust-metrics")
                .param("communityId", communityId.toString())
                .param("period", "LAST_30_DAYS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.communityName").value("Trust District"))
            .andExpect(jsonPath("$.period").value("LAST_30_DAYS"))
            .andExpect(jsonPath("$.lowData").value(false))
            .andExpect(jsonPath("$.freshness").isNotEmpty())
            .andExpect(jsonPath("$.cards[0].key").value("resolution_rate"))
            .andExpect(jsonPath("$.cards[0].value").value("50%"))
            .andExpect(jsonPath("$.cards[1].key").value("participation_coverage"))
            .andExpect(jsonPath("$.cards[1].value").value("100%"))
            .andExpect(jsonPath("$.cards[2].key").value("execution_completion_rate"))
            .andExpect(jsonPath("$.cards[2].value").value("50%"))
            .andExpect(jsonPath("$.cards[3].key").value("median_resolution_hours"))
            .andExpect(jsonPath("$.breakdowns[0].key").value("signals_by_status"))
            .andExpect(jsonPath("$.breakdowns[0].items[0].label").exists())
            .andExpect(jsonPath("$.breakdowns[1].key").value("decisions_by_status"))
            .andExpect(jsonPath("$.breakdowns[2].key").value("tasks_by_stage"))
            .andExpect(jsonPath("$.breakdowns[3].key").value("issue_categories"));
    }

    private CommunityMembership createMembership(UUID communityId, UUID userId, CommunityRole role, UUID createdBy) {
        CommunityMembership membership = new CommunityMembership();
        membership.setCommunityId(communityId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setCreatedBy(createdBy);
        return membership;
    }
}
