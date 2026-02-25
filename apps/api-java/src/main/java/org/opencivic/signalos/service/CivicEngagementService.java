package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.CivicComment;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CivicCommentRepository;
import org.opencivic.signalos.repository.CommunityBlogPostRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CivicCommentResponse;
import org.opencivic.signalos.web.dto.ReactionStateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class CivicEngagementService {

    private final CivicCommentRepository commentRepository;
    private final SignalRepository signalRepository;
    private final CommunityBlogPostRepository blogRepository;
    private final UserRepository userRepository;
    private final CommunityAccessService accessService;
    private final UserReactionService userReactionService;

    public CivicEngagementService(CivicCommentRepository commentRepository, 
                                  SignalRepository signalRepository, 
                                  CommunityBlogPostRepository blogRepository,
                                  UserRepository userRepository,
                                  CommunityAccessService accessService,
                                  UserReactionService userReactionService) {
        this.commentRepository = commentRepository;
        this.signalRepository = signalRepository;
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.userReactionService = userReactionService;
    }

    public List<CivicCommentResponse> getComments(UUID parentId, String parentType) {
        return commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(parentId, parentType)
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    public Map<UUID, Long> getCommentCounts(List<UUID> parentIds, String parentType) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (UUID parentId : parentIds) {
            counts.put(parentId, 0L);
        }

        commentRepository.countByParentIdsAndParentType(parentIds, parentType)
            .forEach(entry -> counts.put(entry.getParentId(), entry.getCommentCount()));

        return counts;
    }

    @Transactional
    public CivicCommentResponse addComment(UUID parentId, String parentType, String content, String username) {
        User user = accessService.getCurrentUser(username);
        CivicComment comment = new CivicComment(parentId, parentType, user.getId(), content);
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public ReactionStateResponse react(UUID parentId, String parentType, String reactionType, String username) {
        User user = accessService.getCurrentUser(username);
        if ("SIGNAL".equalsIgnoreCase(parentType)) {
            Signal signal = signalRepository.findById(parentId).orElseThrow(() -> new ResourceNotFoundException("Signal not found"));
            ReactionStateResponse reactionState = userReactionService.toggleReaction(
                "SIGNAL",
                parentId,
                user.getId(),
                reactionType,
                signal::getReactions
            );
            signal.setReactions(reactionState.reactions());
            signalRepository.save(signal);
            return reactionState;
        } else {
            CommunityBlogPost blog = blogRepository.findById(parentId).orElseThrow(() -> new ResourceNotFoundException("Blog not found"));
            ReactionStateResponse reactionState = userReactionService.toggleReaction(
                "BLOG",
                parentId,
                user.getId(),
                reactionType,
                blog::getReactions
            );
            blog.setReactions(reactionState.reactions());
            blogRepository.save(blog);
            return reactionState;
        }
    }

    private CivicCommentResponse toCommentResponse(CivicComment comment) {
        User author = userRepository.findById(comment.getAuthorId()).orElseThrow();
        return new CivicCommentResponse(
            comment.getId(),
            comment.getParentId(),
            comment.getParentType(),
            comment.getAuthorId(),
            author.getUsername(),
            author.getRoles(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
