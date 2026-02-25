package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.UserReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReactionRepository extends JpaRepository<UserReaction, UUID> {
    Optional<UserReaction> findByParentTypeAndParentIdAndUserId(String parentType, UUID parentId, UUID userId);

    List<UserReaction> findByParentTypeAndParentIdInAndUserId(String parentType, Collection<UUID> parentIds, UUID userId);
}
