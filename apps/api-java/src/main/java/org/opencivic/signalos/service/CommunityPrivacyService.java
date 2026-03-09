package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityOpenDataPolicy;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityPrivacyPolicyResponse;
import org.opencivic.signalos.web.dto.UpdateCommunityPrivacyPolicyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityPrivacyService {
    private final CommunityRepository communityRepository;
    private final CommunityAccessService accessService;
    private final PrivacyAccessLogService privacyAccessLogService;
    private final UserRepository userRepository;

    public CommunityPrivacyService(
        CommunityRepository communityRepository,
        CommunityAccessService accessService,
        PrivacyAccessLogService privacyAccessLogService,
        UserRepository userRepository
    ) {
        this.communityRepository = communityRepository;
        this.accessService = accessService;
        this.privacyAccessLogService = privacyAccessLogService;
        this.userRepository = userRepository;
    }

    public CommunityPrivacyPolicyResponse getPolicy(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_PRIVACY_SETTINGS);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        return toResponse(community);
    }

    @Transactional
    public CommunityPrivacyPolicyResponse updatePolicy(
        UUID communityId,
        UpdateCommunityPrivacyPolicyRequest request,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_PRIVACY_SETTINGS);
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
        community.setOpenDataPolicy(parsePolicy(request.openDataPolicy()));
        community.setPrivacyUpdatedBy(user.getId());
        community.setPrivacyUpdatedAt(LocalDateTime.now());
        communityRepository.save(community);
        return toResponse(community);
    }

    private CommunityPrivacyPolicyResponse toResponse(Community community) {
        String updatedByUsername = community.getPrivacyUpdatedBy() == null
            ? null
            : userRepository.findById(community.getPrivacyUpdatedBy()).map(User::getUsername).orElse("unknown");
        return new CommunityPrivacyPolicyResponse(
            community.getId(),
            community.getName(),
            community.getOpenDataPolicy().name(),
            updatedByUsername,
            community.getPrivacyUpdatedAt(),
            privacyAccessLogService.getLogsForCommunity(community.getId())
        );
    }

    private CommunityOpenDataPolicy parsePolicy(String rawValue) {
        try {
            return CommunityOpenDataPolicy.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Unknown community open-data policy.");
        }
    }
}
