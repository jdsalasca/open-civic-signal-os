package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.ExplainabilitySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExplainabilitySnapshotRepository extends JpaRepository<ExplainabilitySnapshot, UUID> {
    /**
     * Id breaks createdAt ties on purpose. Two snapshots recorded in the same tick share a timestamp -
     * H2 keeps no fractional seconds - and the database is free to return either first, which is how
     * this list came to reorder itself between page loads. The tiebreak makes the order stable.
     *
     * <p>It does not make it newest-first. When two rows claim the same instant, the data does not record
     * which meeting came first, and a random UUID is not that record either. Deciding it properly needs a
     * monotonic position, which is a separate piece of work: the status timeline has one (V50) but the
     * suite never populates it, because {@code ddl-auto: create-drop} replaces the schema Flyway built.
     */
    List<ExplainabilitySnapshot> findByCommunityIdOrderByCreatedAtDescIdDesc(UUID communityId);
    Optional<ExplainabilitySnapshot> findByIdAndCommunityId(UUID id, UUID communityId);
}