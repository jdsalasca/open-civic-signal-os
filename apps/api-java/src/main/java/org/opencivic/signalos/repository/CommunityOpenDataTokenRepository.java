package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityOpenDataToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityOpenDataTokenRepository extends JpaRepository<CommunityOpenDataToken, UUID> {
    List<CommunityOpenDataToken> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<CommunityOpenDataToken> findByIdAndCommunityId(UUID id, UUID communityId);

    Optional<CommunityOpenDataToken> findByTokenHash(String tokenHash);
}
