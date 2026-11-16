package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.opencivic.signalos.domain.ContentFollow;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.ContentFollowRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentFollowService {
    private static final Set<String> TARGET_TYPES = Set.of("SIGNAL", "THREAD", "BLOG");

    private final ContentFollowRepository followRepository;
    private final UserRepository userRepository;

    public ContentFollowService(ContentFollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean follow(String targetType, UUID targetId, String username) {
        User user = requireUser(username);
        String type = normalizeType(targetType);
        if (followRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), type, targetId).isPresent()) {
            return true;
        }
        ContentFollow follow = new ContentFollow();
        follow.setUserId(user.getId());
        follow.setTargetType(type);
        follow.setTargetId(targetId);
        follow.setCreatedAt(LocalDateTime.now());
        try {
            followRepository.save(follow);
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent double click: the unique constraint already made it idempotent.
        }
        return true;
    }

    @Transactional
    public void unfollow(String targetType, UUID targetId, String username) {
        User user = requireUser(username);
        followRepository.deleteByUserIdAndTargetTypeAndTargetId(user.getId(), normalizeType(targetType), targetId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String targetType, UUID targetId, String username) {
        User user = requireUser(username);
        return followRepository.findByUserIdAndTargetTypeAndTargetId(user.getId(), normalizeType(targetType), targetId)
            .isPresent();
    }

    @Transactional(readOnly = true)
    public List<UUID> followerIds(String targetType, UUID targetId) {
        return followRepository.findByTargetTypeAndTargetId(targetType, targetId).stream()
            .map(ContentFollow::getUserId)
            .distinct()
            .toList();
    }

    @Transactional(readOnly = true)
    public Set<UUID> followedIds(UUID userId, String targetType, List<UUID> targetIds) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        return followRepository
            .findByUserIdAndTargetTypeAndTargetIdIn(userId, normalizeType(targetType), targetIds)
            .stream()
            .map(ContentFollow::getTargetId)
            .collect(java.util.stream.Collectors.toSet());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String normalizeType(String targetType) {
        String type = targetType == null ? "" : targetType.trim().toUpperCase();
        if (!TARGET_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unsupported follow target: " + targetType);
        }
        return type;
    }
}
