package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityDecision;
import org.opencivic.signalos.domain.CommunityDecisionBasisType;
import org.opencivic.signalos.domain.CommunityDecisionStatus;
import org.opencivic.signalos.domain.CommunityDecisionType;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.GovernanceDocument;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.GovernanceDocumentRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityDecisionResponse;
import org.opencivic.signalos.web.dto.CreateCommunityDecisionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityDecisionLedgerService {
    private final CommunityAccessService accessService;
    private final CommunityDecisionRepository decisionRepository;
    private final CommunityProposalRepository proposalRepository;
    private final GovernanceDocumentRepository governanceDocumentRepository;
    private final CommunityProjectBoardRepository projectBoardRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CommunityDecisionLedgerService(
        CommunityAccessService accessService,
        CommunityDecisionRepository decisionRepository,
        CommunityProposalRepository proposalRepository,
        GovernanceDocumentRepository governanceDocumentRepository,
        CommunityProjectBoardRepository projectBoardRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.decisionRepository = decisionRepository;
        this.proposalRepository = proposalRepository;
        this.governanceDocumentRepository = governanceDocumentRepository;
        this.projectBoardRepository = projectBoardRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public List<CommunityDecisionResponse> getDecisions(
        UUID communityId,
        String decisionStatus,
        String decisionType,
        LocalDate dateFrom,
        LocalDate dateTo,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        return decisionRepository.findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(communityId).stream()
            .filter(decision -> decisionStatus == null || decisionStatus.isBlank() || decision.getDecisionStatus().name().equalsIgnoreCase(decisionStatus.trim()))
            .filter(decision -> decisionType == null || decisionType.isBlank() || decision.getDecisionType().name().equalsIgnoreCase(decisionType.trim()))
            .filter(decision -> dateFrom == null || !decision.getDecidedAt().toLocalDate().isBefore(dateFrom))
            .filter(decision -> dateTo == null || !decision.getDecidedAt().toLocalDate().isAfter(dateTo))
            .map(this::toResponse)
            .toList();
    }

    public CommunityDecisionResponse getDecision(UUID decisionId, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityDecision decision = getDecisionEntity(decisionId);
        accessService.requireMembership(user.getId(), decision.getCommunityId());
        return toResponse(decision);
    }

    @Transactional
    public CommunityDecisionResponse createDecision(CreateCommunityDecisionRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_DECISION_LEDGER);

        CommunityProposal proposal = validateProposal(request.communityId(), request.linkedProposalId());
        GovernanceDocument governanceDocument = validateGovernanceDocument(request.communityId(), request.governanceDocumentId());
        CommunityProjectBoard projectBoard = validateProjectBoard(request.communityId(), request.projectBoardId());
        if (proposal == null && governanceDocument == null) {
            throw new IllegalArgumentException("Decision ledger entries must link to a proposal or governance document.");
        }

        UUID executionOwnerId = resolveExecutionOwnerId(
            request.communityId(),
            request.executionOwnerUsername(),
            projectBoard == null ? null : projectBoard.getOwnerId()
        );

        CommunityDecision decision = new CommunityDecision();
        decision.setCommunityId(request.communityId());
        decision.setLinkedProposalId(proposal == null ? null : proposal.getId());
        decision.setGovernanceDocumentId(governanceDocument == null ? null : governanceDocument.getId());
        decision.setProjectBoardId(projectBoard == null ? null : projectBoard.getId());
        decision.setDecidedBy(user.getId());
        decision.setExecutionOwnerId(executionOwnerId);
        decision.setDecisionType(CommunityDecisionType.valueOf(request.decisionType().trim().toUpperCase(Locale.ROOT)));
        decision.setDecisionStatus(CommunityDecisionStatus.valueOf(request.decisionStatus().trim().toUpperCase(Locale.ROOT)));
        decision.setApprovalBasisType(CommunityDecisionBasisType.valueOf(request.approvalBasisType().trim().toUpperCase(Locale.ROOT)));
        decision.setTitle(request.title().trim());
        decision.setSummary(request.summary().trim());
        decision.setApprovalBasisSummary(request.approvalBasisSummary().trim());
        decision.setDecidedAt(request.decidedAt() == null ? LocalDateTime.now() : request.decidedAt());
        decision.setEffectiveDate(request.effectiveDate());
        decision.setCreatedAt(LocalDateTime.now());
        decision.setUpdatedAt(LocalDateTime.now());
        return toResponse(decisionRepository.save(decision));
    }

    private UUID resolveExecutionOwnerId(UUID communityId, String executionOwnerUsername, UUID fallbackOwnerId) {
        if (executionOwnerUsername == null || executionOwnerUsername.trim().isBlank()) {
            return fallbackOwnerId;
        }
        User owner = userRepository.findByUsername(executionOwnerUsername.trim())
            .orElseThrow(() -> new IllegalArgumentException("Execution owner username was not found."));
        CommunityMembership membership = membershipRepository.findByUserIdAndCommunityId(owner.getId(), communityId)
            .orElseThrow(() -> new IllegalArgumentException("Execution owner must belong to the same community."));
        return membership.getUserId();
    }

    private CommunityProposal validateProposal(UUID communityId, UUID proposalId) {
        if (proposalId == null) {
            return null;
        }
        CommunityProposal proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + proposalId));
        if (!proposal.getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Linked proposal must belong to the same community.");
        }
        return proposal;
    }

    private GovernanceDocument validateGovernanceDocument(UUID communityId, UUID governanceDocumentId) {
        if (governanceDocumentId == null) {
            return null;
        }
        GovernanceDocument document = governanceDocumentRepository.findById(governanceDocumentId)
            .orElseThrow(() -> new ResourceNotFoundException("Governance document not found: " + governanceDocumentId));
        if (!document.getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Linked governance document must belong to the same community.");
        }
        return document;
    }

    private CommunityProjectBoard validateProjectBoard(UUID communityId, UUID projectBoardId) {
        if (projectBoardId == null) {
            return null;
        }
        CommunityProjectBoard board = projectBoardRepository.findById(projectBoardId)
            .orElseThrow(() -> new ResourceNotFoundException("Community project board not found: " + projectBoardId));
        if (!board.getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Linked project board must belong to the same community.");
        }
        return board;
    }

    private CommunityDecision getDecisionEntity(UUID decisionId) {
        return decisionRepository.findById(decisionId)
            .orElseThrow(() -> new ResourceNotFoundException("Community decision not found: " + decisionId));
    }

    private CommunityDecisionResponse toResponse(CommunityDecision decision) {
        CommunityProposal proposal = decision.getLinkedProposalId() == null ? null : proposalRepository.findById(decision.getLinkedProposalId()).orElse(null);
        GovernanceDocument document = decision.getGovernanceDocumentId() == null ? null : governanceDocumentRepository.findById(decision.getGovernanceDocumentId()).orElse(null);
        CommunityProjectBoard board = decision.getProjectBoardId() == null ? null : projectBoardRepository.findById(decision.getProjectBoardId()).orElse(null);
        User decidedBy = userRepository.findById(decision.getDecidedBy()).orElse(null);
        User executionOwner = decision.getExecutionOwnerId() == null ? null : userRepository.findById(decision.getExecutionOwnerId()).orElse(null);

        return new CommunityDecisionResponse(
            decision.getId(),
            decision.getCommunityId(),
            decision.getLinkedProposalId(),
            proposal == null ? null : proposal.getTitle(),
            decision.getGovernanceDocumentId(),
            document == null ? null : document.getTitle(),
            decision.getProjectBoardId(),
            board == null ? null : board.getTitle(),
            decision.getDecidedBy(),
            decidedBy == null ? "deleted_user" : decidedBy.getUsername(),
            decision.getExecutionOwnerId(),
            executionOwner == null ? null : executionOwner.getUsername(),
            decision.getDecisionType().name(),
            decision.getDecisionStatus().name(),
            decision.getApprovalBasisType().name(),
            decision.getApprovalBasisSummary(),
            decision.getTitle(),
            decision.getSummary(),
            decision.getDecidedAt(),
            decision.getEffectiveDate(),
            decision.getCreatedAt(),
            decision.getUpdatedAt()
        );
    }
}
