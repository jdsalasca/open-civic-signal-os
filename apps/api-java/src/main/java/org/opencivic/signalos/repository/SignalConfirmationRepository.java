package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.SignalConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalConfirmationRepository extends JpaRepository<SignalConfirmation, UUID> {
    Optional<SignalConfirmation> findBySignalIdAndUserId(UUID signalId, UUID userId);

    long countBySignalId(UUID signalId);

    List<SignalConfirmation> findBySignalIdIn(Collection<UUID> signalIds);

    List<SignalConfirmation> findByUserIdAndSignalIdIn(UUID userId, Collection<UUID> signalIds);

    void deleteBySignalIdAndUserId(UUID signalId, UUID userId);
}
