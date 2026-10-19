package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProposalRepository extends JpaRepository<CommunityProposal, UUID> {
    List<CommunityProposal> findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(UUID communityId);
}
