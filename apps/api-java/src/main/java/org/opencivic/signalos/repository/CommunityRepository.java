package org.opencivic.signalos.repository;

import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, UUID> {
    Optional<Community> findBySlug(String slug);
}
