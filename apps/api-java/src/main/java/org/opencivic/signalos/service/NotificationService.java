package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Notification;
import java.util.List;

public interface NotificationService {
    void broadcastTop10();
    List<Notification> getRecentNotifications();
}
