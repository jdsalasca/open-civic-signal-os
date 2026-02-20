package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.opencivic.signalos.domain.CommunityRole;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityCollaborationService {
    private final CommunityAccessService accessService;
    private final CommunityThreadRepository threadRepository;
    private final CommunityThreadMessageRepository messageRepository;
    private final CommunityBlogPostRepository blogPostRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;

    public CommunityCollaborationService(
        CommunityAccessService accessService,
        CommunityThreadRepository threadRepository,
        CommunityThreadMessageRepository messageRepository,
        CommunityBlogPostRepository blogPostRepository,
        SignalRepository signalRepository,
        UserRepository userRepository
    ) {
        this.accessService = accessService;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.blogPostRepository = blogPostRepository;
        this.signalRepository = signalRepository;
        this.userRepository = userRepository;
    }

    public List<CommunityThreadResponse> getThreads(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        return threadRepository.findBySourceCommunityIdOrTargetCommunityIdOrderByUpdatedAtDesc(communityId, communityId)
            .stream()
            .map(this::toThreadResponse)
            .toList();
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
        accessService.requireMembership(user.getId(), sourceCommunityId);
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
        return toThreadResponse(saved);
    }

    @Transactional
    public CommunityThreadMessageResponse addMessage(
        UUID threadId,
        UUID sourceCommunityId,
        String content,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireAnyRole(
            user.getId(),
            sourceCommunityId,
            Set.of(
                CommunityRole.MEMBER,
                CommunityRole.MODERATOR,
                CommunityRole.COORDINATOR,
                CommunityRole.PUBLIC_SERVANT_LIAISON
            )
        );
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
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        CommunityThreadMessage saved = messageRepository.save(message);
        thread.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(thread);
        return toMessageResponse(saved);
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
        boolean canModerateSource = hasModerationRole(user.getId(), thread.getSourceCommunityId());
        boolean canModerateTarget = hasModerationRole(user.getId(), thread.getTargetCommunityId());
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
        return toMessageResponse(saved);
    }

    public List<CommunityBlogPostResponse> getBlogTimeline(UUID communityId, String username) {
        User user = accessService.getCurrentUser(username);
        accessService.requireMembership(user.getId(), communityId);
        List<CommunityBlogPost> posts = blogPostRepository.findByCommunityIdOrderByPublishedAtDesc(communityId);
        
        Set<UUID> authorIds = posts.stream().map(CommunityBlogPost::getAuthorId).collect(Collectors.toSet());
        Map<UUID, User> authors = userRepository.findAllById(authorIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        return posts.stream()
            .map(post -> toBlogResponse(post, authors.get(post.getAuthorId())))
            .toList();
    }

    @Transactional
    public CommunityBlogPostResponse createBlogPost(
        UUID communityId,
        String title,
        String content,
        String statusTag,
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        accessService.requireAnyRole(
            user.getId(),
            communityId,
            Set.of(CommunityRole.PUBLIC_SERVANT_LIAISON, CommunityRole.COORDINATOR)
        );
        CommunityBlogPost post = new CommunityBlogPost();
        post.setCommunityId(communityId);
        post.setAuthorId(user.getId());
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
        String username
    ) {
        User user = accessService.getCurrentUser(username);
        CommunityBlogPost post = blogPostRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Community blog post not found: " + postId));
        accessService.requireAnyRole(
            user.getId(),
            post.getCommunityId(),
            Set.of(CommunityRole.PUBLIC_SERVANT_LIAISON, CommunityRole.COORDINATOR)
        );
        post.setTitle(title);
        post.setContent(content);
        post.setStatusTag(statusTag);
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

        blogPostRepository.findByCommunityIdOrderByPublishedAtDesc(communityId, limited).stream()
            .filter(post -> post.getPublishedAt() != null && post.getPublishedAt().isAfter(since))
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

    private boolean hasModerationRole(UUID userId, UUID communityId) {
        try {
            accessService.requireAnyRole(
                userId,
                communityId,
                Set.of(CommunityRole.MODERATOR, CommunityRole.COORDINATOR)
            );
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private CommunityThreadResponse toThreadResponse(CommunityThread thread) {
        List<CommunityThreadMessageResponse> messages = messageRepository
            .findByThreadIdOrderByCreatedAtAsc(thread.getId())
            .stream()
            .map(this::toMessageResponse)
            .toList();
        return new CommunityThreadResponse(
            thread.getId(),
            thread.getSourceCommunityId(),
            thread.getTargetCommunityId(),
            thread.getRelatedSignalId(),
            thread.getTitle(),
            thread.getCreatedBy(),
            thread.getCreatedAt(),
            thread.getUpdatedAt(),
            messages
        );
    }

    private CommunityThreadMessageResponse toMessageResponse(CommunityThreadMessage message) {
        return new CommunityThreadMessageResponse(
            message.getId(),
            message.getThreadId(),
            message.getAuthorId(),
            message.getSourceCommunityId(),
            message.getContent(),
            message.isHidden(),
            message.getModerationReason(),
            message.getHiddenBy(),
            message.getHiddenAt(),
            message.getCreatedAt()
        );
    }

    private CommunityBlogPostResponse toBlogResponse(CommunityBlogPost post, User author) {
        String username = author != null ? author.getUsername() : "deleted_user";
        String roles = author != null ? author.getRoles() : "";
        return new CommunityBlogPostResponse(
            post.getId(),
            post.getCommunityId(),
            post.getAuthorId(),
            username,
            roles,
            post.getTitle(),
            post.getContent(),
            post.getStatusTag(),
            post.getPublishedAt(),
            post.getUpdatedAt()
        );
    }

    private CommunityBlogPostResponse toBlogResponse(CommunityBlogPost post) {
        User author = userRepository.findById(post.getAuthorId()).orElse(null);
        return toBlogResponse(post, author);
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
}
