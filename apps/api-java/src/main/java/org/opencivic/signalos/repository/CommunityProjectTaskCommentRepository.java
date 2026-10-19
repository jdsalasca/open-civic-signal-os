package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProjectTaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProjectTaskCommentRepository extends JpaRepository<CommunityProjectTaskComment, UUID> {
    List<CommunityProjectTaskComment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
