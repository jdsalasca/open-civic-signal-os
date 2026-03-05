package org.opencivic.signalos.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityThread;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityBlogPostRepository;
import org.opencivic.signalos.repository.CommunityThreadMessageRepository;
import org.opencivic.signalos.repository.CommunityThreadRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityBlogPostResponse;
import org.opencivic.signalos.web.dto.CommunityFeedItemResponse;
import org.opencivic.signalos.web.dto.CommunityThreadMessageResponse;
import org.opencivic.signalos.web.dto.CommunityThreadResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityCollaborationService {
    private static final int THREAD_REPLY_DEPTH_LIMIT = 4;
    private final CommunityAccessService accessService;
    private final CommunityThreadRepository threadRepository;
    private final CommunityThreadMessageRepository messageRepository;
    private final CommunityBlogPostRepository blogPostRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;
    private final UserReactionService userReactionService;

    public CommunityCollaborationService(
        CommunityAccessService accessService,
        CommunityThreadRepository threadRepository,
        CommunityThreadMessageRepository messageRepository,
        CommunityBlogPostRepository blogPostRepository,
        SignalRepository signalRepository,
        UserRepository userRepository,
        UserReactionService userReactionService
    ) {
        this.accessService = accessService;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.blogPostRepository = blogPostRepository;
        this.signalRepository = signalRepository;
        this.userRepository = userRepository;
        this.userReactionService = userReactionService;
    }

    public Page<CommunityThreadResponse> getThreads(
        UUID communityId,
        String statusFilter,
        String sortBy,
        Pageable pageable,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        String normalizedStatus = normalizeThreadStatusFilter(statusFilter);
        String normalizedSort = normalizeThreadSort(sortBy);
        List<CommunityThread> threads = threadRepository.findAllByCommunityAndStatus(
            communityId,
            normalizedStatus,
            LocalDateTime.now().minusDays(7)
        );
        if (threads.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<UUID> threadIds = threads.stream().map(CommunityThread::getId).toList();
        Map<UUID, List<CommunityThreadMessage>> messagesByThreadId = loadThreadMessages(threadIds);
        List<UUID> messageIds = messagesByThreadId.values().stream()
            .flatMap(List::stream)
            .map(CommunityThreadMessage::getId)
            .toList();
        Map<UUID, String> viewerReactions = userReactionService.getViewerReactions(
            "THREAD_MESSAGE",
            messageIds,
            user.getId()
        );

        List<CommunityThreadResponse> responses = threads.stream()
            .map(thread -> toThreadResponse(
                thread,
                messagesByThreadId.getOrDefault(thread.getId(), List.of()),
                viewerReactions
            ))
            .toList();
        List<CommunityThreadResponse> sortedResponses = sortThreadResponses(responses, normalizedSort);
        int fromIndex = Math.min((int) pageable.getOffset(), sortedResponses.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), sortedResponses.size());
        return new PageImpl<>(sortedResponses.subList(fromIndex, toIndex), pageable, sortedResponses.size());
    }

    @Transactional
    public CommunityThreadResponse createThread(
        UUID sourceCommunityId,
        UUID targetCommunityId,
        UUID relatedSignalId,
        String title,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), sourceCommunityId, CommunityPermissionScope.CREATE_THREAD);
        accessService.requireMembership(user.getId(), targetCommunityId);
        CommunityThread thread = new CommunityThread();
        thread.setSourceCommunityId(sourceCommunityId);
        thread.setTargetCommunityId(targetCommunityId);
        thread.setRelatedSignalId(relatedSignalId);
        thread.setTitle(title);
        thread.setCreatedBy(user.getId());
        thread.setCreatedAt(LocalDateTime.now());
        thread.setUpdatedAt(LocalDateTime.now());
        CommunityThread saved = threadRepository.save(thread);
        return toThreadResponse(saved, List.of(), Map.of());
    }

    @Transactional
    public CommunityThreadMessageResponse addMessage(
        UUID threadId,
        UUID sourceCommunityId,
        String content,
        UUID parentMessageId,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), sourceCommunityId, CommunityPermissionScope.ADD_THREAD_MESSAGE);
        CommunityThread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
        if (!thread.getSourceCommunityId().equals(sourceCommunityId)
            && !thread.getTargetCommunityId().equals(sourceCommunityId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Source community is not linked to thread " + threadId
            );
        }
        CommunityThreadMessage message = new CommunityThreadMessage();
        message.setThreadId(threadId);
        message.setAuthorId(user.getId());
        message.setSourceCommunityId(sourceCommunityId);
        if (parentMessageId != null) {
            CommunityThreadMessage parent = messageRepository.findById(parentMessageId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent message not found: " + parentMessageId));
            if (!parent.getThreadId().equals(threadId)) {
                throw new ResourceNotFoundException(
                    "Parent message does not belong to thread: " + parentMessageId
                );
            }
            int parentDepth = resolveThreadMessageDepth(parent);
            if (parentDepth + 1 > THREAD_REPLY_DEPTH_LIMIT) {
                throw new IllegalArgumentException(
                    "Reply depth limit exceeded. Community threads support up to "
                        + THREAD_REPLY_DEPTH_LIMIT
                        + " nested reply levels."
                );
            }
            message.setParentMessageId(parentMessageId);
        }
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        CommunityThreadMessage saved = messageRepository.save(message);
        thread.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(thread);
        return toMessageResponse(saved, null, resolveThreadMessageDepth(saved), 0);
    }

    @Transactional
    public CommunityThreadMessageResponse moderateMessage(
        UUID threadId,
        UUID messageId,
        boolean hidden,
        String reason,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityThread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread not found: " + threadId));
        boolean canModerateSource = hasScope(user.getId(), thread.getSourceCommunityId(), CommunityPermissionScope.MODERATE_THREAD_MESSAGE);
        boolean canModerateTarget = hasScope(user.getId(), thread.getTargetCommunityId(), CommunityPermissionScope.MODERATE_THREAD_MESSAGE);
        if (!canModerateSource && !canModerateTarget) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Moderator or coordinator role required."
            );
        }
        CommunityThreadMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread message not found: " + messageId));
        message.setHidden(hidden);
        message.setModerationReason(reason);
        message.setHiddenBy(user.getId());
        message.setHiddenAt(LocalDateTime.now());
        CommunityThreadMessage saved = messageRepository.save(message);
        thread.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(thread);
        int directReplyCount = (int) messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
            .filter(candidate -> messageId.equals(candidate.getParentMessageId()))
            .count();
        return toMessageResponse(saved, null, resolveThreadMessageDepth(saved), directReplyCount);
    }

    public List<CommunityBlogPostResponse> getBlogTimeline(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        List<CommunityBlogPost> posts = blogPostRepository.findActiveOfficialTimeline(communityId);
        return toBlogResponses(posts, user.getId());
    }

    public List<CommunityBlogPostResponse> getBlogArchive(
        UUID communityId,
        String query,
        LocalDate dateFrom,
        LocalDate dateTo,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        List<CommunityBlogPost> posts = blogPostRepository.findArchivedOfficialTimeline(
            communityId,
            normalizeArchiveQuery(query),
            dateFrom == null ? null : dateFrom.atStartOfDay(),
            dateTo == null ? null : dateTo.plusDays(1).atStartOfDay()
        );
        return toBlogResponses(posts, user.getId());
    }

    private List<CommunityBlogPostResponse> toBlogResponses(List<CommunityBlogPost> posts, UUID viewerId) {
        
        Set<UUID> authorIds = posts.stream().map(CommunityBlogPost::getAuthorId).collect(Collectors.toSet());
        Map<UUID, User> authors = userRepository.findAllById(authorIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, String> viewerReactions = userReactionService.getViewerReactions(
            "BLOG",
            posts.stream().map(CommunityBlogPost::getId).toList(),
            viewerId
        );
        return posts.stream()
            .map(post -> toBlogResponse(post, authors.get(post.getAuthorId()), viewerReactions.get(post.getId())))
            .toList();
    }

    @Transactional
    public CommunityBlogPostResponse createBlogPost(
        UUID communityId,
        String title,
        String content,
        String statusTag,
        boolean pinned,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireScope(user.getId(), communityId, CommunityPermissionScope.CREATE_OFFICIAL_UPDATE);
        CommunityBlogPost post = new CommunityBlogPost();
        post.setCommunityId(communityId);
        post.setAuthorId(user.getId());
        post.setOfficial(true);
        post.setPinned(pinned);
        post.setTitle(title);
        post.setContent(content);
        post.setStatusTag(statusTag);
        post.setPublishedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return toBlogResponse(blogPostRepository.save(post));
    }

    @Transactional
    public CommunityBlogPostResponse updateBlogPost(
        UUID postId,
        String title,
        String content,
        String statusTag,
        boolean pinned,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityBlogPost post = blogPostRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Community blog post not found: " + postId));
        accessService.requireScope(user.getId(), post.getCommunityId(), CommunityPermissionScope.UPDATE_OFFICIAL_UPDATE);
        post.setTitle(title);
        post.setContent(content);
        post.setStatusTag(statusTag);
        post.setPinned(pinned);
        post.setUpdatedAt(LocalDateTime.now());
        return toBlogResponse(blogPostRepository.save(post));
    }

    @Transactional
    public CommunityBlogPostResponse archiveBlogPost(
        UUID postId,
        boolean archived,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityBlogPost post = blogPostRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Community blog post not found: " + postId));
        accessService.requireScope(user.getId(), post.getCommunityId(), CommunityPermissionScope.UPDATE_OFFICIAL_UPDATE);
        if (archived) {
            post.setArchivedAt(LocalDateTime.now());
            post.setArchivedBy(user.getId());
            post.setPinned(false);
        } else {
            post.setArchivedAt(null);
            post.setArchivedBy(null);
        }
        post.setUpdatedAt(LocalDateTime.now());
        return toBlogResponse(blogPostRepository.save(post));
    }

    public List<CommunityFeedItemResponse> getCommunityFeed(UUID communityId, int days, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));

        List<CommunityFeedItemResponse> items = new ArrayList<>();
        PageRequest limited = PageRequest.of(0, 50);

        signalRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, limited).stream()
            .filter(signal -> signal.getCreatedAt() != null && signal.getCreatedAt().isAfter(since))
            .forEach(signal -> items.add(
                new CommunityFeedItemResponse(
                    "signal",
                    signal.getId(),
                    communityId,
                    signal.getTitle(),
                    signal.getStatus(),
                    signal.getCreatedAt(),
                    freshness(signal.getCreatedAt())
                )
            ));

        blogPostRepository.findActiveOfficialTimeline(communityId).stream()
            .filter(post -> post.getPublishedAt() != null && post.getPublishedAt().isAfter(since))
            .limit(limited.getPageSize())
            .forEach(post -> items.add(
                new CommunityFeedItemResponse(
                    "blog",
                    post.getId(),
                    communityId,
                    post.getTitle(),
                    post.getStatusTag(),
                    post.getPublishedAt(),
                    freshness(post.getPublishedAt())
                )
            ));

        threadRepository.findBySourceCommunityIdOrTargetCommunityIdOrderByUpdatedAtDesc(communityId, communityId, limited).stream()
            .filter(thread -> thread.getUpdatedAt() != null && thread.getUpdatedAt().isAfter(since))
            .forEach(thread -> items.add(
                new CommunityFeedItemResponse(
                    "thread-update",
                    thread.getId(),
                    communityId,
                    thread.getTitle(),
                    "cross-community update",
                    thread.getUpdatedAt(),
                    freshness(thread.getUpdatedAt())
                )
            ));

        return items.stream()
            .sorted(Comparator.comparing(CommunityFeedItemResponse::happenedAt).reversed())
            .limit(100)
            .toList();
    }

    private boolean hasScope(UUID userId, UUID communityId, CommunityPermissionScope scope) {
        try {
            accessService.requireScope(userId, communityId, scope);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private CommunityThreadResponse toThreadResponse(
        CommunityThread thread,
        List<CommunityThreadMessage> threadMessages,
        Map<UUID, String> viewerReactions
    ) {
        Map<UUID, Integer> directReplyCounts = buildDirectReplyCountMap(threadMessages);
        List<CommunityThreadMessageResponse> messages = threadMessages
            .stream()
            .map(message -> toMessageResponse(
                message,
                viewerReactions.get(message.getId()),
                resolveThreadMessageDepth(message),
                directReplyCounts.getOrDefault(message.getId(), 0)
            ))
            .toList();
        int totalMessages = threadMessages.size();
        int totalReplies = (int) threadMessages.stream().filter(message -> message.getParentMessageId() != null).count();
        int totalReactions = threadMessages.stream()
            .flatMap(message -> message.getReactions().values().stream())
            .mapToInt(Integer::intValue)
            .sum();
        double relevanceScore = calculateThreadRelevanceScore(thread, totalMessages, totalReplies, totalReactions);
        return new CommunityThreadResponse(
            thread.getId(),
            thread.getSourceCommunityId(),
            thread.getTargetCommunityId(),
            thread.getRelatedSignalId(),
            thread.getTitle(),
            thread.getCreatedBy(),
            thread.getCreatedAt(),
            thread.getUpdatedAt(),
            totalMessages,
            totalReplies,
            totalReactions,
            relevanceScore,
            buildThreadRelevanceSummary(totalReplies, totalReactions, thread.getUpdatedAt()),
            messages
        );
    }

    private Map<UUID, List<CommunityThreadMessage>> loadThreadMessages(List<UUID> threadIds) {
        List<CommunityThreadMessage> allMessages = messageRepository.findByThreadIdInOrderByThreadIdAscCreatedAtAsc(threadIds);
        Map<UUID, List<CommunityThreadMessage>> messagesByThreadId = new LinkedHashMap<>();
        for (CommunityThreadMessage message : allMessages) {
            messagesByThreadId.computeIfAbsent(message.getThreadId(), ignored -> new ArrayList<>()).add(message);
        }
        return messagesByThreadId;
    }

    @Transactional
    public CommunityThreadMessageResponse reactToMessage(
        UUID threadId,
        UUID messageId,
        String reactionType,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityThreadMessage message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Thread message not found: " + messageId));
        if (!message.getThreadId().equals(threadId)) {
            throw new ResourceNotFoundException("Thread message does not belong to thread: " + threadId);
        }

        var reactionState = userReactionService.toggleReaction(
            "THREAD_MESSAGE",
            messageId,
            user.getId(),
            reactionType,
            message::getReactions
        );
        message.setReactions(reactionState.reactions());
        CommunityThreadMessage saved = messageRepository.save(message);
        int directReplyCount = (int) messageRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
            .filter(candidate -> messageId.equals(candidate.getParentMessageId()))
            .count();
        return toMessageResponse(saved, reactionState.viewerReaction(), resolveThreadMessageDepth(saved), directReplyCount);
    }

    private CommunityThreadMessageResponse toMessageResponse(
        CommunityThreadMessage message,
        String viewerReaction,
        int depth,
        int directReplyCount
    ) {
        return new CommunityThreadMessageResponse(
            message.getId(),
            message.getThreadId(),
            message.getAuthorId(),
            message.getSourceCommunityId(),
            message.getParentMessageId(),
            depth,
            directReplyCount,
            message.getContent(),
            message.isHidden(),
            message.getModerationReason(),
            message.getHiddenBy(),
            message.getHiddenAt(),
            message.getCreatedAt(),
            message.getReactions(),
            viewerReaction
        );
    }

    private CommunityBlogPostResponse toBlogResponse(CommunityBlogPost post, User author, String viewerReaction) {
        String username = author != null ? author.getUsername() : "deleted_user";
        String roles = author != null ? author.getRoles() : "";
        return new CommunityBlogPostResponse(
            post.getId(),
            post.getCommunityId(),
            post.getAuthorId(),
            username,
            roles,
            post.isOfficial(),
            post.isPinned(),
            post.getTitle(),
            post.getContent(),
            post.getStatusTag(),
            post.getReactions(),
            viewerReaction,
            post.getArchivedBy(),
            post.getArchivedAt(),
            post.getPublishedAt(),
            post.getUpdatedAt()
        );
    }

    private CommunityBlogPostResponse toBlogResponse(CommunityBlogPost post) {
        User author = userRepository.findById(post.getAuthorId()).orElse(null);
        return toBlogResponse(post, author, null);
    }

    private String freshness(LocalDateTime timestamp) {
        long minutes = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now());
        if (minutes < 1) {
            return "updated just now";
        }
        if (minutes < 60) {
            return "updated " + minutes + "m ago";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "updated " + hours + "h ago";
        }
        return "updated " + (hours / 24) + "d ago";
    }

    private String normalizeThreadStatusFilter(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return null;
        }
        String normalized = statusFilter.trim().toUpperCase();
        if (!normalized.equals("ACTIVE") && !normalized.equals("STALE")) {
            throw new IllegalArgumentException("Invalid thread status filter. Use ACTIVE or STALE.");
        }
        return normalized;
    }

    private String normalizeThreadSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "RELEVANCE";
        }
        String normalized = sortBy.trim().toUpperCase();
        if (!normalized.equals("RELEVANCE") && !normalized.equals("RECENT")) {
            throw new IllegalArgumentException("Invalid thread sort. Use RELEVANCE or RECENT.");
        }
        return normalized;
    }

    private List<CommunityThreadResponse> sortThreadResponses(List<CommunityThreadResponse> responses, String sortBy) {
        Comparator<LocalDateTime> dateComparator = Comparator.nullsLast(Comparator.reverseOrder());
        Comparator<CommunityThreadResponse> comparator;
        if ("RECENT".equals(sortBy)) {
            comparator = Comparator
                .comparing(CommunityThreadResponse::updatedAt, dateComparator)
                .thenComparing(CommunityThreadResponse::createdAt, dateComparator);
        } else {
            comparator = Comparator
                .comparingDouble(CommunityThreadResponse::relevanceScore)
                .reversed()
                .thenComparing(CommunityThreadResponse::updatedAt, dateComparator);
        }
        return responses.stream().sorted(comparator).toList();
    }

    private double calculateThreadRelevanceScore(
        CommunityThread thread,
        int totalMessages,
        int totalReplies,
        int totalReactions
    ) {
        long hoursSinceUpdate = Math.max(0, ChronoUnit.HOURS.between(thread.getUpdatedAt(), LocalDateTime.now()));
        double freshnessBoost = Math.max(0, 48 - hoursSinceUpdate);
        return (totalReactions * 4.0) + (totalReplies * 3.0) + (totalMessages * 2.0) + freshnessBoost;
    }

    private String buildThreadRelevanceSummary(int totalReplies, int totalReactions, LocalDateTime updatedAt) {
        return totalReactions + " reactions, "
            + totalReplies + " replies, "
            + freshness(updatedAt);
    }

    private Map<UUID, Integer> buildDirectReplyCountMap(List<CommunityThreadMessage> messages) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (CommunityThreadMessage message : messages) {
            if (message.getParentMessageId() == null) {
                continue;
            }
            counts.merge(message.getParentMessageId(), 1, Integer::sum);
        }
        return counts;
    }

    private int resolveThreadMessageDepth(CommunityThreadMessage message) {
        int depth = 0;
        UUID parentId = message.getParentMessageId();
        while (parentId != null) {
            UUID currentParentId = parentId;
            CommunityThreadMessage parent = messageRepository.findById(currentParentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent message not found: " + currentParentId));
            depth += 1;
            parentId = parent.getParentMessageId();
        }
        return depth;
    }

    private String normalizeArchiveQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return query.trim();
    }
}
