package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getRecentNotifications();
    void sendTop10Relay();
}
