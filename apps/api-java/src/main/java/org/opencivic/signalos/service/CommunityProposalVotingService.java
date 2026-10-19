package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVote;
import org.opencivic.signalos.domain.CommunityProposalVoteAuditEvent;
import org.opencivic.signalos.domain.CommunityProposalVoteAuditEventType;
import org.opencivic.signalos.domain.CommunityProposalVoteChoice;
import org.opencivic.signalos.domain.CommunityProposalVoteEligibility;
import org.opencivic.signalos.domain.CommunityProposalVoteMode;
import org.opencivic.signalos.domain.CommunityProposalVoteVisibility;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityProposalVoteAuditEventRepository;
import org.opencivic.signalos.repository.CommunityProposalVoteRepository;
import org.opencivic.signalos.web.dto.CastCommunityProposalVoteRequest;
import org.opencivic.signalos.web.dto.CommunityProposalVoteAuditSummaryResponse;
import org.opencivic.signalos.web.dto.CommunityProposalVoteConfigResponse;
import org.opencivic.signalos.web.dto.CommunityProposalVoteRecordResponse;
import org.opencivic.signalos.web.dto.CommunityProposalVoteScoreBucketResponse;
import org.opencivic.signalos.web.dto.CommunityProposalVoteTallyResponse;
import org.opencivic.signalos.web.dto.CommunityProposalVotingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityProposalVotingService {
    private final CommunityAccessService accessService;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityProposalVoteRepository voteRepository;
    private final CommunityProposalVoteAuditEventRepository auditEventRepository;

    public CommunityProposalVotingService(
        CommunityAccessService accessService,
        CommunityMembershipRepository membershipRepository,
        CommunityProposalRepository proposalRepository,
        CommunityProposalVoteRepository voteRepository,
        CommunityProposalVoteAuditEventRepository auditEventRepository
    ) {
        this.accessService = accessService;
        this.membershipRepository = membershipRepository;
        this.proposalRepository = proposalRepository;
        this.voteRepository = voteRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public CommunityProposalVotingResponse getVoting(UUID proposalId, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposal(proposalId);
        accessService.requireMembership(user.getId(), proposal.getCommunityId());
        return buildVotingResponse(proposal, user);
    }

    @Transactional
    public CommunityProposalVotingResponse castVote(UUID proposalId, CastCommunityProposalVoteRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        CommunityProposal proposal = getProposal(proposalId);
        CommunityMembership membership = accessService.requireScope(user.getId(), proposal.getCommunityId(), CommunityPermissionScope.CAST_PROPOSAL_VOTE);

        if (voteRepository.findByProposalIdAndVoterId(proposalId, user.getId()).isPresent()) {
            recordAudit(proposal, user, CommunityProposalVoteAuditEventType.DUPLICATE_BLOCKED, "User already voted on this proposal.");
            throw new ConflictException("User has already voted on this community proposal.");
        }
        if (!isVotingWindowOpen(proposal, LocalDateTime.now())) {
            recordAudit(proposal, user, CommunityProposalVoteAuditEventType.WINDOW_CLOSED_BLOCKED, "Voting window is closed for this proposal.");
            throw new ConflictException("Voting is not open for this proposal right now.");
        }
        if (proposal.getVoteEligibility() == CommunityProposalVoteEligibility.VERIFIED_MEMBERS && !user.isVerified()) {
            recordAudit(proposal, user, CommunityProposalVoteAuditEventType.ELIGIBILITY_BLOCKED, "Verified membership is required for this vote.");
            throw new IllegalArgumentException("Verified membership is required before voting on this proposal.");
        }

        CommunityProposalVote vote = new CommunityProposalVote();
        vote.setProposalId(proposalId);
        vote.setCommunityId(proposal.getCommunityId());
        vote.setVoterId(user.getId());
        vote.setVoterUsername(user.getUsername());
        vote.setMembershipRole(membership.getRole());
        vote.setVerifiedMember(user.isVerified());
        vote.setVoteMode(proposal.getVoteMode());
        vote.setCreatedAt(LocalDateTime.now());
        vote.setUpdatedAt(LocalDateTime.now());
        applyVotePayload(vote, proposal.getVoteMode(), request);
        voteRepository.save(vote);
        recordAudit(proposal, user, CommunityProposalVoteAuditEventType.CAST_ACCEPTED, "Vote recorded successfully.");
        return buildVotingResponse(proposal, user);
    }

    private void applyVotePayload(
        CommunityProposalVote vote,
        CommunityProposalVoteMode voteMode,
        CastCommunityProposalVoteRequest request
    ) {
        if (voteMode == CommunityProposalVoteMode.YES_NO) {
            if (request.choice() == null || request.choice().isBlank()) {
                throw new IllegalArgumentException("Simple proposal voting requires a FOR or AGAINST choice.");
            }
            try {
                vote.setChoice(CommunityProposalVoteChoice.valueOf(request.choice().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Simple proposal voting requires a FOR or AGAINST choice.");
            }
            vote.setScoreValue(null);
            return;
        }
        if (request.scoreValue() == null) {
            throw new IllegalArgumentException("Scored proposal voting requires a score from 1 to 5.");
        }
        if (request.scoreValue() < 1 || request.scoreValue() > 5) {
            throw new IllegalArgumentException("Scored proposal voting requires a score from 1 to 5.");
        }
        vote.setChoice(null);
        vote.setScoreValue(request.scoreValue());
    }

    private CommunityProposalVotingResponse buildVotingResponse(CommunityProposal proposal, User user) {
        List<CommunityProposalVote> votes = voteRepository.findByProposalIdOrderByCreatedAtAsc(proposal.getId());
        CommunityProposalVote currentUserVote = votes.stream()
            .filter(vote -> vote.getVoterId().equals(user.getId()))
            .findFirst()
            .orElse(null);

        boolean openForVoting = isVotingWindowOpen(proposal, LocalDateTime.now());
        String blockedReason = resolveBlockedReason(proposal, user, currentUserVote, openForVoting);
        boolean canCurrentUserVote = blockedReason == null;
        boolean tallyVisible = proposal.getVoteVisibility() == CommunityProposalVoteVisibility.COMMUNITY || currentUserVote != null;

        long totalMembers = membershipRepository.countByCommunityId(proposal.getCommunityId());
        long forVotes = votes.stream().filter(vote -> vote.getChoice() == CommunityProposalVoteChoice.FOR).count();
        long againstVotes = votes.stream().filter(vote -> vote.getChoice() == CommunityProposalVoteChoice.AGAINST).count();
        var averageScoreOptional = votes.stream()
            .map(CommunityProposalVote::getScoreValue)
            .filter(score -> score != null)
            .mapToInt(Integer::intValue)
            .average();
        Double averageScore = averageScoreOptional.isPresent() ? averageScoreOptional.getAsDouble() : null;

        List<CommunityProposalVoteScoreBucketResponse> scoreDistribution = java.util.stream.IntStream.rangeClosed(1, 5)
            .mapToObj(score -> new CommunityProposalVoteScoreBucketResponse(
                score,
                votes.stream().filter(vote -> Integer.valueOf(score).equals(vote.getScoreValue())).count()
            ))
            .toList();

        return new CommunityProposalVotingResponse(
            proposal.getId(),
            new CommunityProposalVoteConfigResponse(
                proposal.getVoteMode().name(),
                proposal.getVoteVisibility().name(),
                proposal.getVoteEligibility().name(),
                proposal.getVotingOpensAt(),
                proposal.getVotingClosesAt()
            ),
            openForVoting,
            canCurrentUserVote,
            blockedReason,
            currentUserVote == null ? null : new CommunityProposalVoteRecordResponse(
                currentUserVote.getVoterId(),
                currentUserVote.getVoterUsername(),
                currentUserVote.getMembershipRole().name(),
                currentUserVote.isVerifiedMember(),
                currentUserVote.getChoice() == null ? null : currentUserVote.getChoice().name(),
                currentUserVote.getScoreValue(),
                currentUserVote.getCreatedAt()
            ),
            new CommunityProposalVoteTallyResponse(
                tallyVisible,
                tallyVisible ? null : "Results for this proposal become visible after you vote.",
                votes.size(),
                votes.size(),
                totalMembers <= 0 ? 0.0d : (votes.size() * 100.0d) / totalMembers,
                tallyVisible ? forVotes : 0L,
                tallyVisible ? againstVotes : 0L,
                tallyVisible ? averageScore : null,
                tallyVisible ? scoreDistribution : List.of()
            ),
            new CommunityProposalVoteAuditSummaryResponse(
                auditEventRepository.countByProposalIdAndEventType(proposal.getId(), CommunityProposalVoteAuditEventType.CAST_ACCEPTED),
                auditEventRepository.countByProposalIdAndEventType(proposal.getId(), CommunityProposalVoteAuditEventType.DUPLICATE_BLOCKED),
                auditEventRepository.countByProposalIdAndEventType(proposal.getId(), CommunityProposalVoteAuditEventType.ELIGIBILITY_BLOCKED),
                auditEventRepository.countByProposalIdAndEventType(proposal.getId(), CommunityProposalVoteAuditEventType.WINDOW_CLOSED_BLOCKED)
            )
        );
    }

    private String resolveBlockedReason(
        CommunityProposal proposal,
        User user,
        CommunityProposalVote currentUserVote,
        boolean openForVoting
    ) {
        if (currentUserVote != null) {
            return "You already voted on this proposal.";
        }
        if (!openForVoting) {
            return "Voting is closed for this proposal.";
        }
        if (proposal.getVoteEligibility() == CommunityProposalVoteEligibility.VERIFIED_MEMBERS && !user.isVerified()) {
            return "This proposal only accepts votes from verified members.";
        }
        return null;
    }

    private boolean isVotingWindowOpen(CommunityProposal proposal, LocalDateTime now) {
        if (proposal.getVotingOpensAt() != null && now.isBefore(proposal.getVotingOpensAt())) {
            return false;
        }
        return proposal.getVotingClosesAt() == null || !now.isAfter(proposal.getVotingClosesAt());
    }

    private void recordAudit(
        CommunityProposal proposal,
        User user,
        CommunityProposalVoteAuditEventType eventType,
        String reason
    ) {
        CommunityProposalVoteAuditEvent auditEvent = new CommunityProposalVoteAuditEvent();
        auditEvent.setProposalId(proposal.getId());
        auditEvent.setCommunityId(proposal.getCommunityId());
        auditEvent.setActorUserId(user.getId());
        auditEvent.setEventType(eventType);
        auditEvent.setReason(reason);
        auditEvent.setCreatedAt(LocalDateTime.now());
        auditEventRepository.save(auditEvent);
    }

    private CommunityProposal getProposal(UUID proposalId) {
        return proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + proposalId));
    }
}
