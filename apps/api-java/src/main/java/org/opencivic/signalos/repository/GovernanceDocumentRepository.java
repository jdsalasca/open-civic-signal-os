package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.GovernanceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceDocumentRepository extends JpaRepository<GovernanceDocument, UUID> {
    List<GovernanceDocument> findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(UUID communityId);
}
