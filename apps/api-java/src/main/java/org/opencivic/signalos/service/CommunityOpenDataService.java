package org.opencivic.signalos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityOpenDataAccessChannel;
import org.opencivic.signalos.domain.CommunityOpenDataAccessLog;
import org.opencivic.signalos.domain.CommunityOpenDataExportType;
import org.opencivic.signalos.domain.CommunityOpenDataFormat;
import org.opencivic.signalos.domain.CommunityOpenDataToken;
import org.opencivic.signalos.domain.CommunityOpenDataTokenScope;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProposal;
import org.opencivic.signalos.domain.CommunityProposalVote;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.exception.TooManyRequestsException;
import org.opencivic.signalos.exception.UnauthorizedActionException;
import org.opencivic.signalos.repository.CommunityDecisionRepository;
import org.opencivic.signalos.repository.CommunityOpenDataAccessLogRepository;
import org.opencivic.signalos.repository.CommunityOpenDataTokenRepository;
import org.opencivic.signalos.repository.CommunityProposalRepository;
import org.opencivic.signalos.repository.CommunityProposalVoteRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityOpenDataAccessLogResponse;
import org.opencivic.signalos.web.dto.CommunityOpenDataCenterResponse;
import org.opencivic.signalos.web.dto.CommunityOpenDataDatasetResponse;
import org.opencivic.signalos.web.dto.CommunityOpenDataExportDefinitionResponse;
import org.opencivic.signalos.web.dto.CommunityOpenDataTokenResponse;
import org.opencivic.signalos.web.dto.CommunityTrustMetricsResponse;
import org.opencivic.signalos.web.dto.CreateCommunityOpenDataTokenRequest;
import org.opencivic.signalos.web.dto.CreateCommunityOpenDataTokenResponse;
import org.opencivic.signalos.web.dto.OpenDataDecisionRecordResponse;
import org.opencivic.signalos.web.dto.OpenDataMetricRecordResponse;
import org.opencivic.signalos.web.dto.OpenDataProposalRecordResponse;
import org.opencivic.signalos.web.dto.OpenDataSignalRecordResponse;
import org.opencivic.signalos.web.dto.OpenDataVoteRecordResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityOpenDataService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_RATE_LIMIT_PER_HOUR = 120;
    private static final List<CommunityOpenDataExportDefinitionResponse> EXPORT_DEFINITIONS = List.of(
        new CommunityOpenDataExportDefinitionResponse("SIGNALS", "EXPORT_SIGNALS", "Community issue registry with priority and intake context.", List.of("CSV", "JSON")),
        new CommunityOpenDataExportDefinitionResponse("PROPOSALS", "EXPORT_PROPOSALS", "Structured community proposals with vote configuration and supporting links.", List.of("CSV", "JSON")),
        new CommunityOpenDataExportDefinitionResponse("VOTES", "EXPORT_VOTES", "Recorded proposal vote activity with mode, choice, and member verification context.", List.of("CSV", "JSON")),
        new CommunityOpenDataExportDefinitionResponse("DECISIONS", "EXPORT_DECISIONS", "Decision-ledger records linked to proposals, governance, and execution ownership.", List.of("CSV", "JSON")),
        new CommunityOpenDataExportDefinitionResponse("METRICS", "EXPORT_METRICS", "Trust metrics cards exported across standard reporting periods.", List.of("CSV", "JSON"))
    );

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final SignalRepository signalRepository;
    private final CommunityProposalRepository proposalRepository;
    private final CommunityProposalVoteRepository voteRepository;
    private final CommunityDecisionRepository decisionRepository;
    private final CommunityTrustMetricsService trustMetricsService;
    private final CommunityOpenDataTokenRepository tokenRepository;
    private final CommunityOpenDataAccessLogRepository accessLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    public CommunityOpenDataService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        UserRepository userRepository,
        SignalRepository signalRepository,
        CommunityProposalRepository proposalRepository,
        CommunityProposalVoteRepository voteRepository,
        CommunityDecisionRepository decisionRepository,
        CommunityTrustMetricsService trustMetricsService,
        CommunityOpenDataTokenRepository tokenRepository,
        CommunityOpenDataAccessLogRepository accessLogRepository,
        PasswordEncoder passwordEncoder,
        ObjectMapper objectMapper,
        RateLimitService rateLimitService
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.signalRepository = signalRepository;
        this.proposalRepository = proposalRepository;
        this.voteRepository = voteRepository;
        this.decisionRepository = decisionRepository;
        this.trustMetricsService = trustMetricsService;
        this.tokenRepository = tokenRepository;
        this.accessLogRepository = accessLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
    }

    public CommunityOpenDataCenterResponse getCenter(UUID communityId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        Community community = getCommunity(communityId);

        List<CommunityOpenDataTokenResponse> tokens = tokenRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .map(this::toTokenResponse)
            .toList();
        List<CommunityOpenDataAccessLogResponse> logs = accessLogRepository.findTop20ByCommunityIdOrderByCreatedAtDesc(communityId).stream()
            .map(this::toAccessLogResponse)
            .toList();

        return new CommunityOpenDataCenterResponse(
            community.getId(),
            community.getName(),
            DEFAULT_RATE_LIMIT_PER_HOUR,
            EXPORT_DEFINITIONS.stream().map(definition -> new CommunityOpenDataDatasetResponse(
                definition.exportType(),
                definition.description(),
                definition.availableFormats(),
                "/api/open-data/" + community.getId() + "/" + definition.exportType().toLowerCase()
            )).toList(),
            tokens,
            logs
        );
    }

    @Transactional
    public CreateCommunityOpenDataTokenResponse createToken(CreateCommunityOpenDataTokenRequest request, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        getCommunity(request.communityId());

        List<CommunityOpenDataTokenScope> scopes = request.scopes().stream()
            .map(value -> CommunityOpenDataTokenScope.valueOf(value.trim().toUpperCase()))
            .distinct()
            .sorted(Comparator.comparing(Enum::name))
            .toList();
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("At least one open-data scope is required.");
        }

        CommunityOpenDataToken token = new CommunityOpenDataToken();
        token.setId(UUID.randomUUID());
        token.setCommunityId(request.communityId());
        token.setLabel(request.label().trim());
        token.setScopes(scopes);
        token.setRateLimitPerHour(request.rateLimitPerHour());
        token.setCreatedBy(user.getId());
        String plainToken = "ocs_" + token.getId() + "_" + randomSecret();
        token.setTokenHash(passwordEncoder.encode(plainToken));
        token.setTokenPrefix(plainToken.substring(0, Math.min(18, plainToken.length())));
        token = tokenRepository.save(token);

        return new CreateCommunityOpenDataTokenResponse(toTokenResponse(token), plainToken);
    }

    @Transactional
    public CommunityOpenDataTokenResponse revokeToken(UUID communityId, UUID tokenId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        CommunityOpenDataToken token = tokenRepository.findByIdAndCommunityId(tokenId, communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Open-data token not found: " + tokenId));
        token.setActive(false);
        token.setRevokedAt(LocalDateTime.now());
        return toTokenResponse(tokenRepository.save(token));
    }

    @Transactional
    public ExportPayload exportForUser(UUID communityId, CommunityOpenDataExportType exportType, CommunityOpenDataFormat format, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), communityId, CommunityPermissionScope.MANAGE_OPEN_DATA_EXPORTS);
        getCommunity(communityId);

        Object dataset = buildDataset(communityId, exportType);
        recordUserAccess(communityId, user.getId(), exportType, format, "Privileged community export downloaded through export center.");

        return buildPayload(communityId, exportType, format, dataset);
    }

    @Transactional
    public TokenApiResult readWithToken(UUID communityId, CommunityOpenDataExportType exportType, String plainToken) {
        TokenAccess tokenAccess = validateToken(communityId, exportType, plainToken);
        Object dataset = buildDataset(communityId, exportType);
        int remaining = Math.max(tokenAccess.token().getRateLimitPerHour() - tokenAccess.usedInWindow() - 1, 0);
        LocalDateTime resetAt = tokenAccess.resetAt();

        recordTokenAccess(tokenAccess.token(), exportType, "Scoped open-data API request accepted.");
        tokenAccess.token().setLastUsedAt(LocalDateTime.now());
        tokenRepository.save(tokenAccess.token());

        return new TokenApiResult(dataset, tokenAccess.token().getRateLimitPerHour(), remaining, resetAt);
    }

    private TokenAccess validateToken(UUID communityId, CommunityOpenDataExportType exportType, String plainToken) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new UnauthorizedActionException("Open-data token is required.");
        }
        String[] parts = plainToken.trim().split("_", 3);
        if (parts.length != 3 || !"ocs".equals(parts[0])) {
            throw new UnauthorizedActionException("Open-data token format is invalid.");
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedActionException("Open-data token format is invalid.");
        }

        CommunityOpenDataToken token = tokenRepository.findByIdAndCommunityId(tokenId, communityId)
            .orElseThrow(() -> new UnauthorizedActionException("Open-data token is invalid for this community."));
        if (!token.isActive() || token.getRevokedAt() != null) {
            throw new UnauthorizedActionException("Open-data token has been revoked.");
        }
        if (!passwordEncoder.matches(plainToken, token.getTokenHash())) {
            throw new UnauthorizedActionException("Open-data token is invalid.");
        }

        CommunityOpenDataTokenScope requiredScope = scopeFor(exportType);
        if (!token.getScopes().contains(requiredScope)) {
            throw new UnauthorizedActionException("Open-data token does not include scope " + requiredScope.name() + ".");
        }

        RateLimitService.RateLimitDecision decision = rateLimitService.tryAcquire(
            "community-open-data:" + token.getId(),
            token.getRateLimitPerHour(),
            Duration.ofHours(1)
        );
        if (!decision.allowed()) {
            LocalDateTime resetAt = java.time.Instant.ofEpochSecond(decision.resetAtEpochSecond())
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDateTime();
            throw new TooManyRequestsException("Open-data token rate limit exceeded.", resetAt);
        }
        LocalDateTime resetAt = java.time.Instant.ofEpochSecond(decision.resetAtEpochSecond())
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDateTime();
        return new TokenAccess(token, token.getRateLimitPerHour() - decision.remaining() - 1, resetAt);
    }

    private Object buildDataset(UUID communityId, CommunityOpenDataExportType exportType) {
        return switch (exportType) {
            case SIGNALS -> buildSignals(communityId);
            case PROPOSALS -> buildProposals(communityId);
            case VOTES -> buildVotes(communityId);
            case DECISIONS -> buildDecisions(communityId);
            case METRICS -> buildMetrics(communityId);
        };
    }

    private List<OpenDataSignalRecordResponse> buildSignals(UUID communityId) {
        return signalRepository.findByCommunityId(communityId).stream()
            .sorted(Comparator.comparing(Signal::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(signal -> new OpenDataSignalRecordResponse(
                signal.getId(),
                signal.getCommunityId(),
                signal.getTitle(),
                signal.getCategory(),
                signal.getStatus(),
                signal.getPriorityScore(),
                signal.getUrgency(),
                signal.getImpact(),
                signal.getAffectedPeople(),
                signal.getCommunityVotes(),
                signal.getLocationLabel(),
                signal.getCreatedAt()
            ))
            .toList();
    }

    private List<OpenDataProposalRecordResponse> buildProposals(UUID communityId) {
        return proposalRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .map(proposal -> new OpenDataProposalRecordResponse(
                proposal.getId(),
                proposal.getCommunityId(),
                proposal.getRelatedSignalId(),
                proposal.getTitle(),
                proposal.getStatus(),
                proposal.getProblemStatement(),
                proposal.getProposedSolution(),
                proposal.getEstimatedCost(),
                proposal.getBeneficiariesSummary(),
                proposal.getSupportingLinks(),
                proposal.getVoteMode().name(),
                proposal.getVoteVisibility().name(),
                proposal.getVoteEligibility().name(),
                proposal.getVotingOpensAt(),
                proposal.getVotingClosesAt(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
            ))
            .toList();
    }

    private List<OpenDataVoteRecordResponse> buildVotes(UUID communityId) {
        Map<UUID, String> proposalTitles = proposalRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .collect(Collectors.toMap(CommunityProposal::getId, CommunityProposal::getTitle));

        return proposalTitles.keySet().stream()
            .flatMap(proposalId -> voteRepository.findByProposalIdOrderByCreatedAtAsc(proposalId).stream())
            .sorted(Comparator.comparing(CommunityProposalVote::getCreatedAt))
            .map(vote -> new OpenDataVoteRecordResponse(
                vote.getId(),
                vote.getCommunityId(),
                vote.getProposalId(),
                proposalTitles.get(vote.getProposalId()),
                vote.getVoteMode().name(),
                vote.getChoice() == null ? null : vote.getChoice().name(),
                vote.getScoreValue(),
                vote.getMembershipRole().name(),
                vote.isVerifiedMember(),
                vote.getCreatedAt()
            ))
            .toList();
    }

    private List<OpenDataDecisionRecordResponse> buildDecisions(UUID communityId) {
        return decisionRepository.findByCommunityIdOrderByDecidedAtDescUpdatedAtDesc(communityId).stream()
            .map(decision -> new OpenDataDecisionRecordResponse(
                decision.getId(),
                decision.getCommunityId(),
                decision.getLinkedProposalId(),
                decision.getGovernanceDocumentId(),
                decision.getProjectBoardId(),
                decision.getTitle(),
                decision.getSummary(),
                decision.getDecisionType().name(),
                decision.getDecisionStatus().name(),
                decision.getApprovalBasisType().name(),
                decision.getApprovalBasisSummary(),
                decision.getDecidedAt(),
                decision.getEffectiveDate(),
                decision.getUpdatedAt()
            ))
            .toList();
    }

    private List<OpenDataMetricRecordResponse> buildMetrics(UUID communityId) {
        return List.of("LAST_7_DAYS", "LAST_30_DAYS", "LAST_90_DAYS").stream()
            .map(period -> trustMetricsService.getTrustMetricsForExport(communityId, period))
            .flatMap(response -> response.cards().stream().map(card -> toMetricRow(response, card)))
            .toList();
    }

    private OpenDataMetricRecordResponse toMetricRow(CommunityTrustMetricsResponse response, org.opencivic.signalos.web.dto.TrustMetricCardResponse card) {
        return new OpenDataMetricRecordResponse(
            response.communityId(),
            response.period(),
            card.key(),
            card.label(),
            card.value(),
            card.unit(),
            card.definition(),
            card.formula(),
            response.freshness(),
            response.lowData(),
            response.generatedAt()
        );
    }

    private ExportPayload buildPayload(UUID communityId, CommunityOpenDataExportType exportType, CommunityOpenDataFormat format, Object dataset) {
        String baseFilename = "community_" + communityId + "_" + exportType.name().toLowerCase();
        return switch (format) {
            case JSON -> {
                try {
                    yield new ExportPayload(baseFilename + ".json", "application/json", objectMapper.writeValueAsBytes(dataset));
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to serialize open-data JSON export.", ex);
                }
            }
            case CSV -> new ExportPayload(baseFilename + ".csv", "text/csv", writeCsv(exportType, dataset));
        };
    }

    private byte[] writeCsv(CommunityOpenDataExportType exportType, Object dataset) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            switch (exportType) {
                case SIGNALS -> {
                    writer.println("id,communityId,title,category,status,priorityScore,urgency,impact,affectedPeople,communityVotes,locationLabel,createdAt");
                    for (OpenDataSignalRecordResponse row : castSignals(dataset)) {
                        writer.println(csv(row.id(), row.communityId(), row.title(), row.category(), row.status(), row.priorityScore(), row.urgency(), row.impact(), row.affectedPeople(), row.communityVotes(), row.locationLabel(), row.createdAt()));
                    }
                }
                case PROPOSALS -> {
                    writer.println("id,communityId,relatedSignalId,title,status,problemStatement,proposedSolution,estimatedCost,beneficiariesSummary,supportingLinks,voteMode,voteVisibility,voteEligibility,votingOpensAt,votingClosesAt,createdAt,updatedAt");
                    for (OpenDataProposalRecordResponse row : castProposals(dataset)) {
                        writer.println(csv(row.id(), row.communityId(), row.relatedSignalId(), row.title(), row.status(), row.problemStatement(), row.proposedSolution(), row.estimatedCost(), row.beneficiariesSummary(), String.join(" | ", row.supportingLinks()), row.voteMode(), row.voteVisibility(), row.voteEligibility(), row.votingOpensAt(), row.votingClosesAt(), row.createdAt(), row.updatedAt()));
                    }
                }
                case VOTES -> {
                    writer.println("id,communityId,proposalId,proposalTitle,voteMode,choice,scoreValue,membershipRole,verifiedMember,createdAt");
                    for (OpenDataVoteRecordResponse row : castVotes(dataset)) {
                        writer.println(csv(row.id(), row.communityId(), row.proposalId(), row.proposalTitle(), row.voteMode(), row.choice(), row.scoreValue(), row.membershipRole(), row.verifiedMember(), row.createdAt()));
                    }
                }
                case DECISIONS -> {
                    writer.println("id,communityId,linkedProposalId,governanceDocumentId,projectBoardId,title,summary,decisionType,decisionStatus,approvalBasisType,approvalBasisSummary,decidedAt,effectiveDate,updatedAt");
                    for (OpenDataDecisionRecordResponse row : castDecisions(dataset)) {
                        writer.println(csv(row.id(), row.communityId(), row.linkedProposalId(), row.governanceDocumentId(), row.projectBoardId(), row.title(), row.summary(), row.decisionType(), row.decisionStatus(), row.approvalBasisType(), row.approvalBasisSummary(), row.decidedAt(), row.effectiveDate(), row.updatedAt()));
                    }
                }
                case METRICS -> {
                    writer.println("communityId,period,key,label,value,unit,definition,formula,freshness,lowData,generatedAt");
                    for (OpenDataMetricRecordResponse row : castMetrics(dataset)) {
                        writer.println(csv(row.communityId(), row.period(), row.key(), row.label(), row.value(), row.unit(), row.definition(), row.formula(), row.freshness(), row.lowData(), row.generatedAt()));
                    }
                }
            }
            writer.flush();
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private List<OpenDataSignalRecordResponse> castSignals(Object dataset) {
        return (List<OpenDataSignalRecordResponse>) dataset;
    }

    @SuppressWarnings("unchecked")
    private List<OpenDataProposalRecordResponse> castProposals(Object dataset) {
        return (List<OpenDataProposalRecordResponse>) dataset;
    }

    @SuppressWarnings("unchecked")
    private List<OpenDataVoteRecordResponse> castVotes(Object dataset) {
        return (List<OpenDataVoteRecordResponse>) dataset;
    }

    @SuppressWarnings("unchecked")
    private List<OpenDataDecisionRecordResponse> castDecisions(Object dataset) {
        return (List<OpenDataDecisionRecordResponse>) dataset;
    }

    @SuppressWarnings("unchecked")
    private List<OpenDataMetricRecordResponse> castMetrics(Object dataset) {
        return (List<OpenDataMetricRecordResponse>) dataset;
    }

    private String csv(Object... values) {
        return java.util.Arrays.stream(values)
            .map(this::escapeCsv)
            .collect(Collectors.joining(","));
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        if (text.contains(",") || text.contains("\"")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private void recordUserAccess(UUID communityId, UUID actorUserId, CommunityOpenDataExportType exportType, CommunityOpenDataFormat format, String note) {
        CommunityOpenDataAccessLog log = new CommunityOpenDataAccessLog();
        log.setCommunityId(communityId);
        log.setActorUserId(actorUserId);
        log.setAccessChannel(CommunityOpenDataAccessChannel.USER_EXPORT);
        log.setExportType(exportType);
        log.setFormat(format);
        log.setNote(note);
        accessLogRepository.save(log);
    }

    private void recordTokenAccess(CommunityOpenDataToken token, CommunityOpenDataExportType exportType, String note) {
        CommunityOpenDataAccessLog log = new CommunityOpenDataAccessLog();
        log.setCommunityId(token.getCommunityId());
        log.setTokenId(token.getId());
        log.setAccessChannel(CommunityOpenDataAccessChannel.API_TOKEN);
        log.setExportType(exportType);
        log.setFormat(CommunityOpenDataFormat.JSON);
        log.setNote(note);
        accessLogRepository.save(log);
    }

    private CommunityOpenDataTokenResponse toTokenResponse(CommunityOpenDataToken token) {
        return new CommunityOpenDataTokenResponse(
            token.getId(),
            token.getLabel(),
            token.getTokenPrefix(),
            token.getScopes().stream().map(Enum::name).toList(),
            token.getRateLimitPerHour(),
            token.isActive(),
            token.getCreatedAt(),
            token.getLastUsedAt(),
            token.getRevokedAt()
        );
    }

    private CommunityOpenDataAccessLogResponse toAccessLogResponse(CommunityOpenDataAccessLog log) {
        String actorUsername = log.getActorUserId() == null ? null : userRepository.findById(log.getActorUserId()).map(User::getUsername).orElse("unknown");
        String tokenLabel = log.getTokenId() == null ? null : tokenRepository.findById(log.getTokenId()).map(CommunityOpenDataToken::getLabel).orElse("revoked token");
        return new CommunityOpenDataAccessLogResponse(
            log.getId(),
            log.getAccessChannel().name(),
            log.getExportType().name(),
            log.getFormat().name(),
            actorUsername,
            tokenLabel,
            log.getNote(),
            log.getCreatedAt()
        );
    }

    private CommunityOpenDataTokenScope scopeFor(CommunityOpenDataExportType exportType) {
        return CommunityOpenDataTokenScope.values()[exportType.ordinal()];
    }

    private String randomSecret() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private Community getCommunity(UUID communityId) {
        return communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found for open-data export: " + communityId));
    }

    public record ExportPayload(String filename, String contentType, byte[] body) {}

    public record TokenApiResult(Object payload, int rateLimitLimit, int rateLimitRemaining, LocalDateTime rateLimitResetAt) {}

    private record TokenAccess(CommunityOpenDataToken token, int usedInWindow, LocalDateTime resetAt) {}
}
