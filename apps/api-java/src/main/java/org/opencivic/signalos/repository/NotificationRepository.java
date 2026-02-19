package org.opencivic.signalos.repository;

import org.opencivic.signalos.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findTop50ByOrderBySentAtDesc();
}
