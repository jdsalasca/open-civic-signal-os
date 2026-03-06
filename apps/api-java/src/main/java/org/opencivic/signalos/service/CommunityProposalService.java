package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityProposalResponse;
import org.opencivic.signalos.web.dto.CreateCommunityProposalRequest;
import org.opencivic.signalos.web.dto.UpdateCommunityProposalRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityProposalService {
    private final CommunityAccessService accessService;
    private final CommunityProposalRepository proposalRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;

    public CommunityProposalService(
        CommunityAccessService accessService,
        CommunityProposalRepository proposalRepository,
        SignalRepository signalRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.proposalRepository = proposalRepository;
        this.signalRepository = signalRepository;
        this.userRepository = userRepository;
    }

    public List<CommunityProposalResponse> getProposals(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        return proposalRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .map(this::toResponse)
            .toList();
    }

    public CommunityProposalResponse getProposal(UUID proposalId, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposalEntity(proposalId);
        accessService.requireMembership(user.getId(), proposal.getCommunityId());
        return toResponse(proposal);
    }

    @Transactional
    public CommunityProposalResponse createProposal(CreateCommunityProposalRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.CREATE_PROPOSAL);
        Signal relatedSignal = validateRelatedSignal(request.communityId(), request.relatedSignalId());

        CommunityProposal proposal = new CommunityProposal();
        proposal.setCommunityId(request.communityId());
        proposal.setAuthorId(user.getId());
        proposal.setRelatedSignalId(relatedSignal == null ? null : relatedSignal.getId());
        applyFields(proposal, request.title(), request.problemStatement(), request.proposedSolution(), request.estimatedCost(), request.beneficiariesSummary(), request.supportingLinks());
        proposal.setCreatedAt(LocalDateTime.now());
        proposal.setUpdatedAt(LocalDateTime.now());
        return toResponse(proposalRepository.save(proposal));
    }

    @Transactional
    public CommunityProposalResponse updateProposal(UUID proposalId, UpdateCommunityProposalRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposalEntity(proposalId);
        CommunityMembership membership = accessService.requireMembership(user.getId(), proposal.getCommunityId());
        boolean canEditOwnProposal = proposal.getAuthorId().equals(user.getId());
        boolean canModerateProposal = membership.getRole() == CommunityRole.COORDINATOR || membership.getRole() == CommunityRole.PUBLIC_SERVANT_LIAISON;
        if (!canEditOwnProposal && !canModerateProposal) {
            throw new AccessDeniedException("Only the proposal author or community coordinators can update this proposal.");
        }
        Signal relatedSignal = validateRelatedSignal(proposal.getCommunityId(), request.relatedSignalId());
        proposal.setRelatedSignalId(relatedSignal == null ? null : relatedSignal.getId());
        applyFields(proposal, request.title(), request.problemStatement(), request.proposedSolution(), request.estimatedCost(), request.beneficiariesSummary(), request.supportingLinks());
        proposal.setUpdatedAt(LocalDateTime.now());
        return toResponse(proposalRepository.save(proposal));
    }

    private void applyFields(
        CommunityProposal proposal,
        String title,
        String problemStatement,
        String proposedSolution,
        String estimatedCost,
        String beneficiariesSummary,
        List<String> supportingLinks
    ) {
        proposal.setTitle(title.trim());
        proposal.setProblemStatement(problemStatement.trim());
        proposal.setProposedSolution(proposedSolution.trim());
        proposal.setEstimatedCost(estimatedCost.trim());
        proposal.setBeneficiariesSummary(beneficiariesSummary.trim());
        proposal.setSupportingLinks(sanitizeLinks(supportingLinks));
    }

    private List<String> sanitizeLinks(List<String> links) {
        if (links == null) {
            return List.of();
        }
        List<String> sanitized = links.stream()
            .map(value -> value == null ? "" : value.trim())
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
        for (String link : sanitized) {
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                throw new IllegalArgumentException("Proposal supporting links must start with http:// or https://");
            }
        }
        return sanitized;
    }

    private Signal validateRelatedSignal(UUID communityId, UUID relatedSignalId) {
        if (relatedSignalId == null) {
            return null;
        }
        return signalRepository.findByIdAndCommunityId(relatedSignalId, communityId)
            .orElseThrow(() -> new IllegalArgumentException("Related issue must exist inside the same active community."));
    }

    private CommunityProposal getProposalEntity(UUID proposalId) {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + proposalId));
    }

    private CommunityProposalResponse toResponse(CommunityProposal proposal) {
        User author = userRepository.findById(proposal.getAuthorId()).orElse(null);
        Signal relatedSignal = proposal.getRelatedSignalId() == null ? null : signalRepository.findById(proposal.getRelatedSignalId()).orElse(null);
        return new CommunityProposalResponse(
            proposal.getId(),
            proposal.getCommunityId(),
            proposal.getAuthorId(),
            author == null ? "deleted_user" : author.getUsername(),
            proposal.getRelatedSignalId(),
            relatedSignal == null ? null : relatedSignal.getTitle(),
            proposal.getTitle(),
            proposal.getTemplateKey(),
            proposal.getStatus(),
            proposal.getProblemStatement(),
            proposal.getProposedSolution(),
            proposal.getEstimatedCost(),
            proposal.getBeneficiariesSummary(),
            proposal.getSupportingLinks(),
            proposal.getCreatedAt(),
            proposal.getUpdatedAt()
        );
    }
}
