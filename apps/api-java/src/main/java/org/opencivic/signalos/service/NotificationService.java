package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Notification;
import java.util.List;

public interface NotificationService {
    // P1-10: Nomenclatura unificada para consistencia con el Controller
    void sendTop10Relay();
    List<Notification> getRecentNotifications();
}
