package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByUserIdAndSignalId(UUID userId, UUID signalId);
    long countBySignalId(UUID signalId);
}
