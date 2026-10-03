package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.InstitutionalTicketHandoff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionalTicketHandoffRepository
    extends JpaRepository<InstitutionalTicketHandoff, UUID> {

    List<InstitutionalTicketHandoff> findByCommunityIdOrderByHandedOffAtDesc(UUID communityId);

    Optional<InstitutionalTicketHandoff> findBySignalIdAndTicketRef(UUID signalId, String ticketRef);

    List<InstitutionalTicketHandoff> findByCommunityIdAndResolvedAtIsNull(UUID communityId);
}