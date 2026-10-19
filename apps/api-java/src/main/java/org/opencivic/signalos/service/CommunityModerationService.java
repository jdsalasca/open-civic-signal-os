package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityModerationReasonCode;
import org.opencivic.signalos.domain.CommunityModerationReport;
import org.opencivic.signalos.domain.CommunityModerationReportStatus;
import org.opencivic.signalos.domain.CommunityModerationTargetType;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalDeliberationEntry;
import org.opencivic.signalos.domain.CommunitySanction;
import org.opencivic.signalos.domain.CommunitySanctionStatus;
import org.opencivic.signalos.domain.CommunitySanctionType;
import org.opencivic.signalos.domain.CommunityThread;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ConflictException;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityModerationReportRepository;
import org.opencivic.signalos.repository.CommunityProposalDeliberationEntryRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunitySanctionRepository;
import org.opencivic.signalos.repository.CommunityThreadMessageRepository;
import org.opencivic.signalos.repository.CommunityThreadRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityModerationActionResponse;
import org.opencivic.signalos.web.dto.CommunityModerationQueueResponse;
import org.opencivic.signalos.web.dto.CommunityModerationReportResponse;
import org.opencivic.signalos.web.dto.CommunitySanctionResponse;
import org.opencivic.signalos.web.dto.CreateCommunityModerationReportRequest;
import org.opencivic.signalos.web.dto.ModerateCommunityReportRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityModerationService {
    private final CommunityAccessService accessService;
    private final CommunityModerationReportRepository reportRepository;
    private final CommunitySanctionRepository sanctionRepository;
    private final CommunityThreadRepository threadRepository;
    private final CommunityThreadMessageRepository threadMessageRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityProposalDeliberationEntryRepository proposalEntryRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public CommunityModerationService(
        CommunityAccessService accessService,
        CommunityModerationReportRepository reportRepository,
        CommunitySanctionRepository sanctionRepository,
        CommunityThreadRepository threadRepository,
        CommunityThreadMessageRepository threadMessageRepository,
        CommunityProposalRepository proposalRepository,
        CommunityProposalDeliberationEntryRepository proposalEntryRepository,
        CommunityMembershipRepository membershipRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.reportRepository = reportRepository;
        this.sanctionRepository = sanctionRepository;
        this.threadRepository = threadRepository;
        this.threadMessageRepository = threadMessageRepository;
        this.proposalRepository = proposalRepository;
        this.proposalEntryRepository = proposalEntryRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommunityModerationReportResponse createReport(CreateCommunityModerationReportRequest request, String username) {
        User reporter = accessService.getCurrentUser(username);
        accessService.requireMembership(reporter.getId(), request.communityId());
        TargetContext targetContext = resolveTargetContext(request.communityId(), request.targetType(), request.targetId());
        if (targetContext.authorId().equals(reporter.getId())) {
            throw new IllegalArgumentException("Users cannot report their own content.");
        }
        if (reportRepository.existsByCommunityIdAndTargetTypeAndTargetIdAndReporterUserIdAndStatus(
            request.communityId(),
            request.targetType(),
            request.targetId(),
            reporter.getId(),
            CommunityModerationReportStatus.OPEN
        )) {
            throw new ConflictException("An open moderation report from this user already exists for the selected content.");
        }

        CommunityModerationReport report = new CommunityModerationReport();
        report.setCommunityId(request.communityId());
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setReporterUserId(reporter.getId());
        report.setReportedUserId(targetContext.authorId());
        report.setReasonCode(request.reasonCode());
        report.setDetails(request.details().trim());
        report.setTargetContentPreview(targetContext.preview());
        report.setFalsePositiveReviewRecommended(shouldRecommendFalsePositiveReview(request.reasonCode()));
        report.setCreatedAt(LocalDateTime.now());
        return toReportResponse(reportRepository.save(report));
    }

    public CommunityModerationQueueResponse getQueue(UUID communityId, String status, String targetType, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_MODERATION_QUEUE);
        CommunityModerationReportStatus normalizedStatus = normalizeStatus(status);
        CommunityModerationTargetType normalizedTargetType = normalizeTargetType(targetType);

        List<CommunityModerationReportResponse> reports = reportRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .filter(report -> normalizedStatus == null || report.getStatus() == normalizedStatus)
            .filter(report -> normalizedTargetType == null || report.getTargetType() == normalizedTargetType)
            .map(this::toReportResponse)
            .sorted(Comparator.comparing(CommunityModerationReportResponse::createdAt).reversed())
            .toList();

        return new CommunityModerationQueueResponse(
            communityId,
            reportRepository.countByCommunityIdAndStatus(communityId, CommunityModerationReportStatus.OPEN),
            reportRepository.countByCommunityIdAndStatus(communityId, CommunityModerationReportStatus.ACTIONED),
            reportRepository.countByCommunityIdAndStatus(communityId, CommunityModerationReportStatus.DISMISSED),
            sanctionRepository.countByCommunityIdAndStatus(communityId, CommunitySanctionStatus.ACTIVE),
            reports
        );
    }

    @Transactional
    public CommunityModerationReportResponse resolveReport(UUID reportId, ModerateCommunityReportRequest request, String username) {
        User moderator = accessService.getCurrentUser(username);
        CommunityModerationReport report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ResourceNotFoundException("Community moderation report not found: " + reportId));
        accessService.requireScope(moderator.getId(), report.getCommunityId(), CommunityPermissionScope.MANAGE_MODERATION_QUEUE);
        if (report.getStatus() != CommunityModerationReportStatus.OPEN) {
            throw new ConflictException("This moderation report was already resolved.");
        }

        String normalizedAction = request.action().trim().toUpperCase(Locale.ROOT);
        if (!normalizedAction.equals("DISMISS") && !normalizedAction.equals("ENFORCE")) {
            throw new IllegalArgumentException("Moderation action must be DISMISS or ENFORCE.");
        }

        report.setResolvedByUserId(moderator.getId());
        report.setResolutionReason(request.resolutionReason().trim());
        report.setResolvedAt(LocalDateTime.now());

        if (normalizedAction.equals("DISMISS")) {
            report.setStatus(CommunityModerationReportStatus.DISMISSED);
            return toReportResponse(reportRepository.save(report));
        }

        CommunitySanction sanction = null;
        if (request.sanctionType() != null && !request.sanctionType().trim().isBlank()) {
            sanction = createSanction(report, moderator, request.sanctionType().trim(), request.resolutionReason().trim());
            report.setLinkedSanctionId(sanction.getId());
        }
        if (request.hideContent()) {
            hideTargetContent(report, moderator, request.resolutionReason().trim());
            report.setContentHidden(true);
        }
        report.setStatus(CommunityModerationReportStatus.ACTIONED);
        return toReportResponse(reportRepository.save(report));
    }

    private CommunitySanction createSanction(
        CommunityModerationReport report,
        User moderator,
        String sanctionTypeValue,
        String resolutionReason
    ) {
        CommunitySanctionType sanctionType;
        try {
            sanctionType = CommunitySanctionType.valueOf(sanctionTypeValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown moderation sanction type.");
        }
        CommunitySanction sanction = new CommunitySanction();
        sanction.setCommunityId(report.getCommunityId());
        sanction.setTargetUserId(report.getReportedUserId());
        sanction.setIssuedByUserId(moderator.getId());
        sanction.setReportId(report.getId());
        sanction.setSanctionType(sanctionType);
        sanction.setStatus(CommunitySanctionStatus.ACTIVE);
        sanction.setReason(resolutionReason);
        sanction.setStartsAt(LocalDateTime.now());
        sanction.setEndsAt(resolveSanctionEnd(sanctionType));
        sanction.setCreatedAt(LocalDateTime.now());
        return sanctionRepository.save(sanction);
    }

    private LocalDateTime resolveSanctionEnd(CommunitySanctionType sanctionType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (sanctionType) {
            case WARN -> now.plus(14, ChronoUnit.DAYS);
            case LIMIT_POSTING_7_DAYS, SUSPEND_7_DAYS -> now.plus(7, ChronoUnit.DAYS);
            case SUSPEND_30_DAYS -> now.plus(30, ChronoUnit.DAYS);
        };
    }

    private void hideTargetContent(CommunityModerationReport report, User moderator, String reason) {
        if (report.getTargetType() == CommunityModerationTargetType.THREAD_MESSAGE) {
            CommunityThreadMessage message = threadMessageRepository.findById(report.getTargetId())
                .orElseThrow(() -> new ResourceNotFoundException("Thread message not found: " + report.getTargetId()));
            message.setHidden(true);
            message.setModerationReason(reason);
            message.setHiddenBy(moderator.getId());
            message.setHiddenAt(LocalDateTime.now());
            threadMessageRepository.save(message);
            return;
        }
        CommunityProposalDeliberationEntry entry = proposalEntryRepository.findById(report.getTargetId())
            .orElseThrow(() -> new ResourceNotFoundException("Proposal deliberation entry not found: " + report.getTargetId()));
        entry.setHidden(true);
        entry.setModerationReason(reason);
        entry.setHiddenBy(moderator.getId());
        entry.setHiddenAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        proposalEntryRepository.save(entry);
    }

    private TargetContext resolveTargetContext(UUID communityId, CommunityModerationTargetType targetType, UUID targetId) {
        if (targetType == CommunityModerationTargetType.THREAD_MESSAGE) {
            CommunityThreadMessage message = threadMessageRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread message not found: " + targetId));
            CommunityThread thread = threadRepository.findById(message.getThreadId())
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + message.getThreadId()));
            if (!thread.getSourceCommunityId().equals(communityId) && !thread.getTargetCommunityId().equals(communityId)) {
                throw new IllegalArgumentException("Thread message must belong to the selected community context.");
            }
            return new TargetContext(message.getAuthorId(), sanitizePreview(message.getContent()));
        }
        CommunityProposalDeliberationEntry entry = proposalEntryRepository.findById(targetId)
            .orElseThrow(() -> new ResourceNotFoundException("Proposal deliberation entry not found: " + targetId));
        CommunityProposal proposal = proposalRepository.findById(entry.getProposalId())
            .orElseThrow(() -> new ResourceNotFoundException("Community proposal not found: " + entry.getProposalId()));
        if (!proposal.getCommunityId().equals(communityId)) {
            throw new IllegalArgumentException("Proposal deliberation entry must belong to the selected community.");
        }
        return new TargetContext(entry.getAuthorId(), sanitizePreview(entry.getContent()));
    }

    private String sanitizePreview(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 237) + "...";
    }

    private boolean shouldRecommendFalsePositiveReview(CommunityModerationReasonCode reasonCode) {
        return reasonCode == CommunityModerationReasonCode.MISINFORMATION || reasonCode == CommunityModerationReasonCode.OTHER;
    }

    private CommunityModerationReportStatus normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return CommunityModerationReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown moderation status filter.");
        }
    }

    private CommunityModerationTargetType normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        try {
            return CommunityModerationTargetType.valueOf(targetType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown moderation target filter.");
        }
    }

    private CommunityModerationReportResponse toReportResponse(CommunityModerationReport report) {
        User reporter = userRepository.findById(report.getReporterUserId()).orElse(null);
        User reported = userRepository.findById(report.getReportedUserId()).orElse(null);
        User resolver = report.getResolvedByUserId() == null ? null : userRepository.findById(report.getResolvedByUserId()).orElse(null);
        CommunitySanction sanction = report.getLinkedSanctionId() == null ? null : sanctionRepository.findById(report.getLinkedSanctionId()).orElse(null);

        List<CommunityModerationActionResponse> actionHistory = new ArrayList<>();
        actionHistory.add(new CommunityModerationActionResponse(
            "REPORT_CREATED",
            reporter == null ? "deleted_user" : reporter.getUsername(),
            "Report filed for " + report.getReasonCode().name().toLowerCase(Locale.ROOT).replace('_', ' '),
            report.getCreatedAt()
        ));
        if (report.isContentHidden()) {
            actionHistory.add(new CommunityModerationActionResponse(
                "CONTENT_HIDDEN",
                resolver == null ? "system" : resolver.getUsername(),
                report.getResolutionReason(),
                report.getResolvedAt()
            ));
        }
        if (sanction != null) {
            actionHistory.add(new CommunityModerationActionResponse(
                "SANCTION_ISSUED",
                resolveUsername(sanction.getIssuedByUserId()),
                sanction.getSanctionType().name(),
                sanction.getCreatedAt()
            ));
        }
        if (report.getStatus() == CommunityModerationReportStatus.DISMISSED || report.getStatus() == CommunityModerationReportStatus.ACTIONED) {
            actionHistory.add(new CommunityModerationActionResponse(
                report.getStatus().name(),
                resolver == null ? "deleted_user" : resolver.getUsername(),
                report.getResolutionReason(),
                report.getResolvedAt()
            ));
        }

        return new CommunityModerationReportResponse(
            report.getId(),
            report.getCommunityId(),
            report.getTargetType().name(),
            report.getTargetId(),
            report.getTargetContentPreview(),
            report.getReporterUserId(),
            reporter == null ? "deleted_user" : reporter.getUsername(),
            report.getReportedUserId(),
            reported == null ? "deleted_user" : reported.getUsername(),
            report.getReasonCode().name(),
            report.getDetails(),
            report.getStatus().name(),
            report.isContentHidden(),
            report.isFalsePositiveReviewRecommended(),
            report.getResolutionReason(),
            resolver == null ? null : resolver.getUsername(),
            sanction == null ? null : toSanctionResponse(sanction),
            actionHistory.stream().filter(action -> action.happenedAt() != null).sorted(Comparator.comparing(CommunityModerationActionResponse::happenedAt)).toList(),
            report.getCreatedAt(),
            report.getResolvedAt()
        );
    }

    private CommunitySanctionResponse toSanctionResponse(CommunitySanction sanction) {
        return new CommunitySanctionResponse(
            sanction.getId(),
            sanction.getSanctionType().name(),
            sanction.getStatus().name(),
            sanction.getReason(),
            sanction.getTargetUserId(),
            resolveUsername(sanction.getTargetUserId()),
            sanction.getIssuedByUserId(),
            resolveUsername(sanction.getIssuedByUserId()),
            sanction.getStartsAt(),
            sanction.getEndsAt(),
            sanction.getSanctionType() != CommunitySanctionType.WARN && sanction.getStatus() == CommunitySanctionStatus.ACTIVE
        );
    }

    private String resolveUsername(UUID userId) {
        return userRepository.findById(userId).map(User::getUsername).orElse("deleted_user");
    }

    private record TargetContext(UUID authorId, String preview) {}
}