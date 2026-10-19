package org.opencivic.signalos.repository;

import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposalVoteAuditEvent;
import org.opencivic.signalos.domain.CommunityProposalVoteAuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProposalVoteAuditEventRepository extends JpaRepository<CommunityProposalVoteAuditEvent, UUID> {
    long countByProposalIdAndEventType(UUID proposalId, CommunityProposalVoteAuditEventType eventType);
}
