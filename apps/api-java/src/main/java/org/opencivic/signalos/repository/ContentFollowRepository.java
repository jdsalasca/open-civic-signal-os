package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.ContentFollow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentFollowRepository extends JpaRepository<ContentFollow, UUID> {
    Optional<ContentFollow> findByUserIdAndTargetTypeAndTargetId(UUID userId, String targetType, UUID targetId);

    List<ContentFollow> findByTargetTypeAndTargetId(String targetType, UUID targetId);

    List<ContentFollow> findByUserIdAndTargetTypeAndTargetIdIn(
        UUID userId,
        String targetType,
        Collection<UUID> targetIds
    );

    void deleteByUserIdAndTargetTypeAndTargetId(UUID userId, String targetType, UUID targetId);

    long countByUserId(UUID userId);
}
