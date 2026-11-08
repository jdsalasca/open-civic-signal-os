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
        List<CivicComment> comments = commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(parentId, parentType);
        if (comments.isEmpty()) {
            return List.of();
        }
        // One query for every author in the thread instead of one per comment.
        Map<UUID, User> authors = userRepository.findAllById(
            comments.stream().map(CivicComment::getAuthorId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, user -> user));
        return comments.stream()
                .map(comment -> toCommentResponse(comment, authors.get(comment.getAuthorId())))
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
        return toCommentResponse(comment, userRepository.findById(comment.getAuthorId()).orElse(null));
    }

    private CivicCommentResponse toCommentResponse(CivicComment comment, User author) {
        // An author row can be gone (account deletion) while the comment stays in the record.
        // That used to turn the whole thread into a 500.
        String username = author == null ? "Deleted account" : author.getUsername();
        String roles = author == null ? "" : author.getRoles();
        return new CivicCommentResponse(
            comment.getId(),
            comment.getParentId(),
            comment.getParentType(),
            comment.getAuthorId(),
            username,
            roles,
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
