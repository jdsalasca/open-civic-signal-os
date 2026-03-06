package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityProjectTaskRepository extends JpaRepository<CommunityProjectTask, UUID> {
    List<CommunityProjectTask> findByProjectBoardIdOrderBySortOrderAscCreatedAtAsc(UUID projectBoardId);
}
