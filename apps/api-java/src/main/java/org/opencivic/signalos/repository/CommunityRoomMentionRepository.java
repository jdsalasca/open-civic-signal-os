package org.opencivic.signalos.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMention;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMentionRepository extends JpaRepository<CommunityRoomMention, UUID> {
    List<CommunityRoomMention> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    List<CommunityRoomMention> findByMessageId(UUID messageId);

    /**
     * The paged message read. Callers rendering a page need every mention of every message on it, and
     * asking per message turns one screen into one query per row - up to 200 on a full page.
     */
    List<CommunityRoomMention> findByMessageIdIn(Collection<UUID> messageIds);
    List<CommunityRoomMention> findByMentionedUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID mentionedUserId);
    List<CommunityRoomMention> findByMentionedUserIdAndCommunityIdAndReadAtIsNullOrderByCreatedAtDesc(
        UUID mentionedUserId,
        UUID communityId
    );
    long countByRoomIdAndMentionedUserIdAndReadAtIsNull(UUID roomId, UUID mentionedUserId);
    long countByMentionedUserIdAndCommunityIdAndReadAtIsNull(UUID mentionedUserId, UUID communityId);
}