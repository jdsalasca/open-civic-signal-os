package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityActivityRepository extends JpaRepository<CommunityActivity, UUID> {
    List<CommunityActivity> findByCommunityIdAndCancelledFalseOrderByStartsAtAsc(UUID communityId);

    List<CommunityActivity> findByCancelledFalseAndReminderSentAtIsNullAndStartsAtBetween(
        LocalDateTime from,
        LocalDateTime to
    );
}