package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityThreadMessageRepository extends JpaRepository<CommunityThreadMessage, UUID> {
    List<CommunityThreadMessage> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
}
