package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.FormulaChangeProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FormulaChangeProposalRepository extends JpaRepository<FormulaChangeProposal, UUID> {

    // Explicit queries rather than derived method names. The derivation
    // `findAllByOrderByProposedAtDesc` resolved at startup and then threw
    // InvalidDataAccessApiUsageException at call time with a null message, which is the least
    // debuggable failure shape there is. Writing the query removes the indirection.
    @Query("SELECT p FROM FormulaChangeProposal p ORDER BY p.proposedAt DESC")
    List<FormulaChangeProposal> findAllNewestFirst();

    @Query("SELECT p FROM FormulaChangeProposal p WHERE p.status = :status ORDER BY p.proposedAt DESC")
    List<FormulaChangeProposal> findByStatusNewestFirst(@Param("status") String status);
}