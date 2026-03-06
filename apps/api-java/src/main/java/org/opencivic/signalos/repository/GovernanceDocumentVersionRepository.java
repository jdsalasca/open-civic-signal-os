package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.GovernanceDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GovernanceDocumentVersionRepository extends JpaRepository<GovernanceDocumentVersion, UUID> {
    List<GovernanceDocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);
    Optional<GovernanceDocumentVersion> findByDocumentIdAndVersionNumber(UUID documentId, Integer versionNumber);
}
