package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityBlogPostRepository extends JpaRepository<CommunityBlogPost, UUID> {
    List<CommunityBlogPost> findByCommunityIdOrderByPublishedAtDesc(UUID communityId);
}
