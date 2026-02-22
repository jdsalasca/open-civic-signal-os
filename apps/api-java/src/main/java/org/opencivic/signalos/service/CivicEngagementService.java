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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CivicEngagementService {

    private final CivicCommentRepository commentRepository;
    private final SignalRepository signalRepository;
    private final CommunityBlogPostRepository blogRepository;
    private final UserRepository userRepository;
    private final CommunityAccessService accessService;

    public CivicEngagementService(CivicCommentRepository commentRepository, 
                                  SignalRepository signalRepository, 
                                  CommunityBlogPostRepository blogRepository,
                                  UserRepository userRepository,
                                  CommunityAccessService accessService) {
        this.commentRepository = commentRepository;
        this.signalRepository = signalRepository;
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    public List<CivicCommentResponse> getComments(UUID parentId, String parentType) {
        return commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(parentId, parentType)
                .stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CivicCommentResponse addComment(UUID parentId, String parentType, String content, String username) {
        User user = accessService.getCurrentUser(username);
        CivicComment comment = new CivicComment(parentId, parentType, user.getId(), content);
        return toCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public Map<String, Integer> react(UUID parentId, String parentType, String reactionType) {
        if ("SIGNAL".equalsIgnoreCase(parentType)) {
            Signal signal = signalRepository.findById(parentId).orElseThrow(() -> new ResourceNotFoundException("Signal not found"));
            Map<String, Integer> reactions = signal.getReactions();
            reactions.put(reactionType, reactions.getOrDefault(reactionType, 0) + 1);
            signal.setReactions(reactions);
            return signalRepository.save(signal).getReactions();
        } else {
            CommunityBlogPost blog = blogRepository.findById(parentId).orElseThrow(() -> new ResourceNotFoundException("Blog not found"));
            Map<String, Integer> reactions = blog.getReactions();
            reactions.put(reactionType, reactions.getOrDefault(reactionType, 0) + 1);
            blog.setReactions(reactions);
            return blogRepository.save(blog).getReactions();
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
