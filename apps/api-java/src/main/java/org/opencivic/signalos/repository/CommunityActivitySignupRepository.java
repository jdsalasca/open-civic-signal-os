package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityActivitySignup;
import org.opencivic.signalos.domain.CommunityActivitySignupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityActivitySignupRepository extends JpaRepository<CommunityActivitySignup, UUID> {
    List<CommunityActivitySignup> findByActivityIdOrderByCreatedAtAsc(UUID activityId);
    Optional<CommunityActivitySignup> findByActivityIdAndVolunteerId(UUID activityId, UUID volunteerId);
    long countByActivityIdAndStatus(UUID activityId, CommunityActivitySignupStatus status);
    List<CommunityActivitySignup> findByVolunteerIdAndStatusOrderByCreatedAtDesc(
        UUID volunteerId,
        CommunityActivitySignupStatus status
    );
}