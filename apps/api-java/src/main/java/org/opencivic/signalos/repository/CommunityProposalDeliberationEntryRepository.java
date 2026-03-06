package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposalDeliberationEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProposalDeliberationEntryRepository extends JpaRepository<CommunityProposalDeliberationEntry, UUID> {
    List<CommunityProposalDeliberationEntry> findByProposalIdOrderByCreatedAtAsc(UUID proposalId);
}
