package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityAlertSubscription;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityAlertSubscriptionRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityAlertService {
    private final CommunityAlertSubscriptionRepository alertRepository;
    private final UserRepository userRepository;
    private final CommunityAccessService accessService;

    public CommunityAlertService(
        CommunityAlertSubscriptionRepository alertRepository,
        UserRepository userRepository,
        CommunityAccessService accessService
    ) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    @Transactional
    public boolean subscribe(UUID communityId, String username) {
        User user = requireUser(username);
        accessService.requireMembership(user.getId(), communityId);
        if (alertRepository.findByUserIdAndCommunityId(user.getId(), communityId).isPresent()) {
            return true;
        }
        CommunityAlertSubscription subscription = new CommunityAlertSubscription();
        subscription.setUserId(user.getId());
        subscription.setCommunityId(communityId);
        subscription.setCreatedAt(LocalDateTime.now());
        try {
            alertRepository.save(subscription);
        } catch (DataIntegrityViolationException ignored) {
            // A double click is already idempotent through the unique constraint.
        }
        return true;
    }

    @Transactional
    public void unsubscribe(UUID communityId, String username) {
        User user = requireUser(username);
        alertRepository.deleteByUserIdAndCommunityId(user.getId(), communityId);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(UUID communityId, String username) {
        User user = requireUser(username);
        return alertRepository.findByUserIdAndCommunityId(user.getId(), communityId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<UUID> subscriberIds(UUID communityId) {
        return alertRepository.findByCommunityId(communityId).stream()
            .map(CommunityAlertSubscription::getUserId)
            .distinct()
            .toList();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
