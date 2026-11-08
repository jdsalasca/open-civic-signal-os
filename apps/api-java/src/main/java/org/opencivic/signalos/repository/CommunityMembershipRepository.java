package org.opencivic.signalos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {
    List<CommunityMembership> findByUserId(UUID userId);
    List<CommunityMembership> findByCommunityId(UUID communityId);

    /**
     * The ids, without the membership rows. A community with 500 members loads 500 entities to build
     * the mentionable-username list, when every caller wants one column from each.
     */
    @Query("select m.userId from CommunityMembership m where m.communityId = :communityId")
    List<UUID> findUserIdsByCommunityId(@Param("communityId") UUID communityId);
    Optional<CommunityMembership> findByUserIdAndCommunityId(UUID userId, UUID communityId);
    long countByCommunityId(UUID communityId);

    /**
     * Member counts for every community in one query, for the public directory.
     *
     * <p>Counting per community in a loop is one query per city on a page that lists them all. This
     * returns the whole table's counts at once so the landing page stays a single read.
     */
    @Query("select m.communityId, count(m) from CommunityMembership m group by m.communityId")
    List<Object[]> countGroupedByCommunity();
    long countByUserId(UUID userId);
    long countByUserIdAndRoleIn(UUID userId, List<CommunityRole> roles);
}
