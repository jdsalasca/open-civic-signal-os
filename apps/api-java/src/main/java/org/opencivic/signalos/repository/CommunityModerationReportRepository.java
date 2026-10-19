package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityModerationReport;
import org.opencivic.signalos.domain.CommunityModerationReportStatus;
import org.opencivic.signalos.domain.CommunityModerationTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityModerationReportRepository extends JpaRepository<CommunityModerationReport, UUID> {
    List<CommunityModerationReport> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    long countByCommunityIdAndStatus(UUID communityId, CommunityModerationReportStatus status);
    boolean existsByCommunityIdAndTargetTypeAndTargetIdAndReporterUserIdAndStatus(
        UUID communityId,
        CommunityModerationTargetType targetType,
        UUID targetId,
        UUID reporterUserId,
        CommunityModerationReportStatus status
    );
}