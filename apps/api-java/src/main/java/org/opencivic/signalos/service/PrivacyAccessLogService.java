package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.SensitiveDataAccessLog;
import org.opencivic.signalos.domain.SensitiveDataAccessType;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SensitiveDataAccessLogRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.SensitiveDataAccessLogResponse;
import org.springframework.stereotype.Service;

@Service
public class PrivacyAccessLogService {
    private final SensitiveDataAccessLogRepository accessLogRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;

    public PrivacyAccessLogService(
        SensitiveDataAccessLogRepository accessLogRepository,
        UserRepository userRepository,
        CommunityRepository communityRepository
    ) {
        this.accessLogRepository = accessLogRepository;
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
    }

    public void recordProfileAdminView(User actor, User target, UUID communityId, List<String> revealedFields) {
        SensitiveDataAccessLog log = new SensitiveDataAccessLog();
        log.setActorUserId(actor.getId());
        log.setTargetUserId(target.getId());
        log.setCommunityId(communityId);
        log.setAccessType(SensitiveDataAccessType.PROFILE_ADMIN_VIEW);
        log.setNote("Admin-scoped profile review exposed " + String.join(", ", revealedFields) + ".");
        log.setCreatedAt(LocalDateTime.now());
        accessLogRepository.save(log);
    }

    public void recordSignalExport(User actor) {
        SensitiveDataAccessLog log = new SensitiveDataAccessLog();
        log.setActorUserId(actor.getId());
        log.setAccessType(SensitiveDataAccessType.SIGNAL_EXPORT);
        log.setNote("Sensitive signal export generated through the CSV export endpoint.");
        log.setCreatedAt(LocalDateTime.now());
        accessLogRepository.save(log);
    }

    public List<SensitiveDataAccessLogResponse> getLogsForUser(UUID userId) {
        List<SensitiveDataAccessLog> combined = new ArrayList<>();
        combined.addAll(accessLogRepository.findTop20ByTargetUserIdOrderByCreatedAtDesc(userId));
        combined.addAll(accessLogRepository.findTop20ByActorUserIdOrderByCreatedAtDesc(userId));
        return combined.stream()
            .sorted(Comparator.comparing(SensitiveDataAccessLog::getCreatedAt).reversed())
            .distinct()
            .limit(20)
            .map(this::toResponse)
            .toList();
    }

    public List<SensitiveDataAccessLogResponse> getLogsForCommunity(UUID communityId) {
        return accessLogRepository.findTop20ByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .map(this::toResponse)
            .toList();
    }

    private SensitiveDataAccessLogResponse toResponse(SensitiveDataAccessLog log) {
        return new SensitiveDataAccessLogResponse(
            log.getId(),
            log.getAccessType().name(),
            log.getActorUserId(),
            resolveUsername(log.getActorUserId()),
            log.getTargetUserId(),
            resolveUsername(log.getTargetUserId()),
            log.getCommunityId(),
            resolveCommunityName(log.getCommunityId()),
            log.getNote(),
            log.getCreatedAt()
        );
    }

    private String resolveUsername(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getUsername).orElse("unknown");
    }

    private String resolveCommunityName(UUID communityId) {
        if (communityId == null) {
            return null;
        }
        return communityRepository.findById(communityId).map(Community::getName).orElse("unknown");
    }
}
