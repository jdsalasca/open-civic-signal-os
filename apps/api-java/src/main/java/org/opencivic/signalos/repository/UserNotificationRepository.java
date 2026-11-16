package org.opencivic.signalos.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    List<UserNotification> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update UserNotification n set n.readAt = :readAt where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
