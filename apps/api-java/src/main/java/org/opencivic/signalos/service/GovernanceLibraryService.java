package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityRole;
import org.opencivic.signalos.domain.GovernanceDocument;
import org.opencivic.signalos.domain.GovernanceDocumentType;
import org.opencivic.signalos.domain.GovernanceDocumentVersion;
import org.opencivic.signalos.domain.GovernanceDocumentVisibility;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.GovernanceDocumentRepository;
import org.opencivic.signalos.repository.GovernanceDocumentVersionRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CreateGovernanceDocumentRequest;
import org.opencivic.signalos.web.dto.CreateGovernanceDocumentVersionRequest;
import org.opencivic.signalos.web.dto.GovernanceDocumentResponse;
import org.opencivic.signalos.web.dto.GovernanceDocumentVersionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovernanceLibraryService {
    private final CommunityAccessService accessService;
    private final GovernanceDocumentRepository documentRepository;
    private final GovernanceDocumentVersionRepository versionRepository;
    private final UserRepository userRepository;

    public GovernanceLibraryService(
        CommunityAccessService accessService,
        GovernanceDocumentRepository documentRepository,
        GovernanceDocumentVersionRepository versionRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.userRepository = userRepository;
    }

    public List<GovernanceDocumentResponse> getDocuments(
        UUID communityId,
        String documentType,
        String visibility,
        String query,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityMembership membership = accessService.requireMembership(user.getId(), communityId);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return documentRepository.findByCommunityIdOrderByUpdatedAtDescCreatedAtDesc(communityId).stream()
            .filter(document -> canViewDocument(document, membership.getRole()))
            .filter(document -> documentType == null || documentType.isBlank() || document.getDocumentType().name().equalsIgnoreCase(documentType.trim()))
            .filter(document -> visibility == null || visibility.isBlank() || document.getVisibility().name().equalsIgnoreCase(visibility.trim()))
            .filter(document -> normalizedQuery.isBlank() || matchesQuery(document, normalizedQuery))
            .map(this::toResponse)
            .toList();
    }

    public GovernanceDocumentResponse getDocument(UUID documentId, String username) {
        User user = accessService.getCurrentUser(username);
        GovernanceDocument document = getDocumentEntity(documentId);
        CommunityMembership membership = accessService.requireMembership(user.getId(), document.getCommunityId());
        if (!canViewDocument(document, membership.getRole())) {
            throw new org.springframework.security.access.AccessDeniedException("Document visibility denied for current role.");
        }
        return toResponse(document);
    }

    @Transactional
    public GovernanceDocumentResponse createDocument(CreateGovernanceDocumentRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_GOVERNANCE_LIBRARY);

        GovernanceDocument document = new GovernanceDocument();
        document.setCommunityId(request.communityId());
        document.setCreatedBy(user.getId());
        document.setTitle(request.title().trim());
        document.setSummary(request.summary().trim());
        document.setDocumentType(GovernanceDocumentType.valueOf(request.documentType().trim().toUpperCase(Locale.ROOT)));
        document.setVisibility(GovernanceDocumentVisibility.valueOf(request.visibility().trim().toUpperCase(Locale.ROOT)));
        document.setTags(normalizeTags(request.tags()));
        document.setCurrentVersionNumber(1);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        GovernanceDocument savedDocument = documentRepository.save(document);

        GovernanceDocumentVersion version = new GovernanceDocumentVersion();
        version.setDocumentId(savedDocument.getId());
        version.setCreatedBy(user.getId());
        version.setVersionNumber(1);
        version.setContent(request.content().trim());
        version.setChangeSummary(request.changeSummary().trim());
        version.setSourceUrl(trimToNull(request.sourceUrl()));
        version.setEffectiveDate(request.effectiveDate());
        version.setMeetingDate(request.meetingDate());
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);

        return toResponse(savedDocument);
    }

    @Transactional
    public GovernanceDocumentResponse addVersion(UUID documentId, CreateGovernanceDocumentVersionRequest request, String username) {
        User user = accessService.getCurrentUser(username);
        GovernanceDocument document = getDocumentEntity(documentId);
        accessService.requireScope(user.getId(), document.getCommunityId(), CommunityPermissionScope.MANAGE_GOVERNANCE_LIBRARY);

        int nextVersion = document.getCurrentVersionNumber() + 1;
        GovernanceDocumentVersion version = new GovernanceDocumentVersion();
        version.setDocumentId(documentId);
        version.setCreatedBy(user.getId());
        version.setVersionNumber(nextVersion);
        version.setContent(request.content().trim());
        version.setChangeSummary(request.changeSummary().trim());
        version.setSourceUrl(trimToNull(request.sourceUrl()));
        version.setEffectiveDate(request.effectiveDate());
        version.setMeetingDate(request.meetingDate());
        version.setCreatedAt(LocalDateTime.now());
        versionRepository.save(version);

        document.setCurrentVersionNumber(nextVersion);
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
        return toResponse(document);
    }

    private boolean matchesQuery(GovernanceDocument document, String query) {
        return document.getTitle().toLowerCase(Locale.ROOT).contains(query)
            || document.getSummary().toLowerCase(Locale.ROOT).contains(query)
            || document.getTags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(query));
    }

    private boolean canViewDocument(GovernanceDocument document, CommunityRole role) {
        return switch (document.getVisibility()) {
            case PUBLIC, COMMUNITY -> true;
            case ADMINS -> Set.of(CommunityRole.COORDINATOR, CommunityRole.PUBLIC_SERVANT_LIAISON).contains(role);
        };
    }

    private GovernanceDocumentResponse toResponse(GovernanceDocument document) {
        List<GovernanceDocumentVersion> versions = versionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId());
        GovernanceDocumentVersion currentVersion = versions.stream()
            .filter(version -> version.getVersionNumber().equals(document.getCurrentVersionNumber()))
            .findFirst()
            .orElseGet(() -> versions.isEmpty() ? null : versions.get(0));
        User author = userRepository.findById(document.getCreatedBy()).orElse(null);

        return new GovernanceDocumentResponse(
            document.getId(),
            document.getCommunityId(),
            document.getCreatedBy(),
            author == null ? "deleted_user" : author.getUsername(),
            document.getTitle(),
            document.getSummary(),
            document.getDocumentType().name(),
            document.getVisibility().name(),
            document.getTags(),
            document.getCurrentVersionNumber(),
            currentVersion == null ? null : toVersionResponse(currentVersion),
            versions.stream().map(this::toVersionResponse).toList(),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }

    private GovernanceDocumentVersionResponse toVersionResponse(GovernanceDocumentVersion version) {
        User author = userRepository.findById(version.getCreatedBy()).orElse(null);
        return new GovernanceDocumentVersionResponse(
            version.getId(),
            version.getDocumentId(),
            version.getCreatedBy(),
            author == null ? "deleted_user" : author.getUsername(),
            version.getVersionNumber(),
            version.getContent(),
            version.getChangeSummary(),
            version.getSourceUrl(),
            version.getEffectiveDate(),
            version.getMeetingDate(),
            version.getCreatedAt()
        );
    }

    private GovernanceDocument getDocumentEntity(UUID documentId) {
        return documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Governance document not found: " + documentId));
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream()
            .map(tag -> tag == null ? "" : tag.trim())
            .filter(tag -> !tag.isBlank())
            .distinct()
            .collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
