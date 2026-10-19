package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.DigestScheduleRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestScheduleRunRepository extends JpaRepository<DigestScheduleRun, UUID> {
    Optional<DigestScheduleRun> findByCommunityIdAndWeekKey(UUID communityId, String weekKey);
    List<DigestScheduleRun> findByCommunityIdOrderByWeekKeyDesc(UUID communityId);
}