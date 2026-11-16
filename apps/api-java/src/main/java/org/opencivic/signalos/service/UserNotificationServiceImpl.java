package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.domain.UserNotification;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.UserNotificationRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.UserNotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {
    private final UserNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public UserNotificationServiceImpl(
        UserNotificationRepository notificationRepository,
        UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void notifyUser(
        UUID recipientId,
        UUID actorId,
        String actorName,
        String type,
        String targetType,
        UUID targetId,
        String targetTitle,
        UUID communityId
    ) {
        if (recipientId == null || Objects.equals(recipientId, actorId)) {
            return;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(recipientId);
        notification.setActorId(actorId);
        notification.setActorName(actorName);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setTargetTitle(targetTitle);
        notification.setCommunityId(communityId);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> inbox(String username) {
        User user = requireUser(username);
        return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        User user = requireUser(username);
        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Override
    @Transactional
    public void markRead(UUID notificationId, String username) {
        User user = requireUser(username);
        UserNotification notification = notificationRepository
            .findByIdAndUserId(notificationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllRead(String username) {
        User user = requireUser(username);
        notificationRepository.markAllRead(user.getId(), LocalDateTime.now());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private UserNotificationResponse toResponse(UserNotification notification) {
        return new UserNotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getActorName(),
            notification.getTargetType(),
            notification.getTargetId(),
            notification.getTargetTitle(),
            notification.getCommunityId(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
