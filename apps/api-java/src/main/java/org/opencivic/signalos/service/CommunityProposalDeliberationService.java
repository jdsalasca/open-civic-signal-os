package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalDeliberationEntry;
import org.opencivic.signalos.domain.CommunityProposalDeliberationType;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityProposalDeliberationEntryRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityProposalDeliberationCountsResponse;
import org.opencivic.signalos.web.dto.CommunityProposalDeliberationEntryResponse;
import org.opencivic.signalos.web.dto.CommunityProposalDeliberationResponse;
import org.opencivic.signalos.web.dto.CreateCommunityProposalDeliberationRequest;
import org.opencivic.signalos.web.dto.ModerateCommunityProposalEntryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityProposalDeliberationService {
    private final CommunityAccessService accessService;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityProposalDeliberationEntryRepository entryRepository;
    private final UserRepository userRepository;

    public CommunityProposalDeliberationService(
        CommunityAccessService accessService,
        CommunityProposalRepository proposalRepository,
        CommunityProposalDeliberationEntryRepository entryRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.proposalRepository = proposalRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    public CommunityProposalDeliberationResponse getDeliberation(UUID proposalId, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposal(proposalId);
        accessService.requireMembership(user.getId(), proposal.getCommunityId());
        List<CommunityProposalDeliberationEntry> entries = entryRepository.findByProposalIdOrderByCreatedAtAsc(proposalId);
        return toResponse(proposalId, entries);
    }

    @Transactional
    public CommunityProposalDeliberationResponse createEntry(
        UUID proposalId,
        CreateCommunityProposalDeliberationRequest request,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposal(proposalId);
        accessService.requireScope(user.getId(), proposal.getCommunityId(), CommunityPermissionScope.ADD_THREAD_MESSAGE);

        CommunityProposalDeliberationEntry entry = new CommunityProposalDeliberationEntry();
        entry.setProposalId(proposalId);
        entry.setAuthorId(user.getId());
        entry.setEntryType(request.type());
        entry.setContent(request.content().trim());
        entry.setSupportingLink(sanitizeLink(request.supportingLink(), request.type()));
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entryRepository.save(entry);
        return getDeliberation(proposalId, username);
    }

    @Transactional
    public CommunityProposalDeliberationResponse moderateEntry(
        UUID proposalId,
        UUID entryId,
        ModerateCommunityProposalEntryRequest request,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposal(proposalId);
        accessService.requireScope(user.getId(), proposal.getCommunityId(), CommunityPermissionScope.MODERATE_THREAD_MESSAGE);
        CommunityProposalDeliberationEntry entry = entryRepository.findById(entryId)
            .orElseThrow(() -> new ResourceNotFoundException("Proposal deliberation entry not found: " + entryId));
        if (!entry.getProposalId().equals(proposalId)) {
            throw new IllegalArgumentException("Deliberation entry does not belong to the selected proposal.");
        }

        entry.setHidden(request.hidden());
        entry.setModerationReason(request.reason().trim());
        entry.setHiddenBy(user.getId());
        entry.setHiddenAt(request.hidden() ? LocalDateTime.now() : null);
        entry.setUpdatedAt(LocalDateTime.now());
        entryRepository.save(entry);
        return getDeliberation(proposalId, username);
    }

    private String sanitizeLink(String supportingLink, CommunityProposalDeliberationType type) {
        if (supportingLink == null || supportingLink.trim().isBlank()) {
            if (type == CommunityProposalDeliberationType.EVIDENCE) {
                throw new IllegalArgumentException("Evidence entries require one supporting link.");
            }
            return null;
        }
        String sanitized = supportingLink.trim();
        if (!sanitized.startsWith("http://") && !sanitized.startsWith("https://")) {
            throw new IllegalArgumentException("Supporting link must start with http:// or https://");
        }
        return sanitized;
    }

    private CommunityProposal getProposal(UUID proposalId) {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + proposalId));
    }

    private CommunityProposalDeliberationResponse toResponse(UUID proposalId, List<CommunityProposalDeliberationEntry> entries) {
        int pros = 0;
        int cons = 0;
        int questions = 0;
        int evidence = 0;
        int hiddenEntries = 0;
        for (CommunityProposalDeliberationEntry entry : entries) {
            if (entry.isHidden()) {
                hiddenEntries++;
                continue;
            }
            switch (entry.getEntryType()) {
                case PRO -> pros++;
                case CON -> cons++;
                case QUESTION -> questions++;
                case EVIDENCE -> evidence++;
            }
        }
        List<CommunityProposalDeliberationEntryResponse> entryResponses = entries.stream()
            .map(this::toEntryResponse)
            .toList();
        return new CommunityProposalDeliberationResponse(
            proposalId,
            new CommunityProposalDeliberationCountsResponse(
                pros,
                cons,
                questions,
                evidence,
                pros + cons + questions + evidence,
                hiddenEntries
            ),
            entryResponses
        );
    }

    private CommunityProposalDeliberationEntryResponse toEntryResponse(CommunityProposalDeliberationEntry entry) {
        String authorUsername = userRepository.findById(entry.getAuthorId()).map(User::getUsername).orElse("deleted_user");
        String hiddenByUsername = entry.getHiddenBy() == null
            ? null
            : userRepository.findById(entry.getHiddenBy()).map(User::getUsername).orElse("deleted_user");
        return new CommunityProposalDeliberationEntryResponse(
            entry.getId(),
            entry.getProposalId(),
            entry.getAuthorId(),
            authorUsername,
            entry.getEntryType(),
            entry.getContent(),
            entry.getSupportingLink(),
            entry.isHidden(),
            entry.getModerationReason(),
            hiddenByUsername,
            entry.getHiddenAt(),
            entry.getCreatedAt(),
            entry.getUpdatedAt()
        );
    }
}
