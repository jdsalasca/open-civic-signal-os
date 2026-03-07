package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposalVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProposalVoteRepository extends JpaRepository<CommunityProposalVote, UUID> {
    Optional<CommunityProposalVote> findByProposalIdAndVoterId(UUID proposalId, UUID voterId);

    List<CommunityProposalVote> findByProposalIdOrderByCreatedAtAsc(UUID proposalId);
}
