package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.UserReaction;
import org.opencivic.signalos.repository.UserReactionRepository;
import org.opencivic.signalos.web.dto.ReactionStateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserReactionService {
    private final UserReactionRepository userReactionRepository;

    public UserReactionService(UserReactionRepository userReactionRepository) {
        this.userReactionRepository = userReactionRepository;
    }

    @Transactional
    public ReactionStateResponse toggleReaction(
        String parentType,
        UUID parentId,
        UUID userId,
        String reactionType,
        Supplier<Map<String, Integer>> mapLoader
    ) {
        String normalizedParentType = normalizeParentType(parentType);
        String normalizedReactionType = normalizeReactionType(reactionType);

        Map<String, Integer> reactions = ensureMutable(mapLoader.get());
        UserReaction existing = userReactionRepository
            .findByParentTypeAndParentIdAndUserId(normalizedParentType, parentId, userId)
            .orElse(null);

        if (existing == null) {
            reactions.put(normalizedReactionType, reactions.getOrDefault(normalizedReactionType, 0) + 1);
            UserReaction next = new UserReaction();
            next.setParentType(normalizedParentType);
            next.setParentId(parentId);
            next.setUserId(userId);
            next.setReactionType(normalizedReactionType);
            next.setUpdatedAt(LocalDateTime.now());
            userReactionRepository.save(next);
            return new ReactionStateResponse(reactions, normalizedReactionType);
        }

        String previous = existing.getReactionType();
        if (previous.equals(normalizedReactionType)) {
            decrement(reactions, previous);
            userReactionRepository.delete(existing);
            return new ReactionStateResponse(reactions, null);
        }

        decrement(reactions, previous);
        reactions.put(normalizedReactionType, reactions.getOrDefault(normalizedReactionType, 0) + 1);
        existing.setReactionType(normalizedReactionType);
        existing.setUpdatedAt(LocalDateTime.now());
        userReactionRepository.save(existing);
        return new ReactionStateResponse(reactions, normalizedReactionType);
    }

    @Transactional(readOnly = true)
    public String getViewerReaction(String parentType, UUID parentId, UUID userId) {
        if (userId == null || parentId == null) {
            return null;
        }
        return userReactionRepository
            .findByParentTypeAndParentIdAndUserId(normalizeParentType(parentType), parentId, userId)
            .map(UserReaction::getReactionType)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<UUID, String> getViewerReactions(String parentType, Collection<UUID> parentIds, UUID userId) {
        if (userId == null || parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }
        return userReactionRepository
            .findByParentTypeAndParentIdInAndUserId(normalizeParentType(parentType), parentIds, userId)
            .stream()
            .collect(Collectors.toMap(UserReaction::getParentId, UserReaction::getReactionType));
    }

    private Map<String, Integer> ensureMutable(Map<String, Integer> reactions) {
        return reactions == null ? new HashMap<>() : new HashMap<>(reactions);
    }

    private void decrement(Map<String, Integer> reactions, String key) {
        int current = reactions.getOrDefault(key, 0);
        if (current <= 1) {
            reactions.remove(key);
            return;
        }
        reactions.put(key, current - 1);
    }

    private String normalizeReactionType(String reactionType) {
        if (reactionType == null || reactionType.isBlank()) {
            throw new IllegalArgumentException("Reaction type is required.");
        }
        return reactionType.trim();
    }

    private String normalizeParentType(String parentType) {
        if (parentType == null || parentType.isBlank()) {
            throw new IllegalArgumentException("Parent type is required.");
        }
        return parentType.trim().toUpperCase();
    }
}
