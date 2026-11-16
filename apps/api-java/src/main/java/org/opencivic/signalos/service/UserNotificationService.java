package org.opencivic.signalos.service;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.web.dto.UserNotificationResponse;

public interface UserNotificationService {
    /**
     * Creates a personal notification unless the recipient is the actor.
     *
     * <p>The payload is structured on purpose: the client localizes it, so the backend never stores
     * English prose that a Spanish reader would see.
     */
    void notifyUser(
        UUID recipientId,
        UUID actorId,
        String actorName,
        String type,
        String targetType,
        UUID targetId,
        String targetTitle,
        UUID communityId
    );

    List<UserNotificationResponse> inbox(String username);

    long unreadCount(String username);

    void markRead(UUID notificationId, String username);

    void markAllRead(String username);
}
