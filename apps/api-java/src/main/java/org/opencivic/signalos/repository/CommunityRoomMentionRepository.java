package org.opencivic.signalos.repository;

import java.util.List;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityRoomMention;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRoomMentionRepository extends JpaRepository<CommunityRoomMention, UUID> {
    List<CommunityRoomMention> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    List<CommunityRoomMention> findByMessageId(UUID messageId);
    List<CommunityRoomMention> findByMentionedUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID mentionedUserId);
    List<CommunityRoomMention> findByMentionedUserIdAndCommunityIdAndReadAtIsNullOrderByCreatedAtDesc(
        UUID mentionedUserId,
        UUID communityId
    );
    long countByRoomIdAndMentionedUserIdAndReadAtIsNull(UUID roomId, UUID mentionedUserId);
    long countByMentionedUserIdAndCommunityIdAndReadAtIsNull(UUID mentionedUserId, UUID communityId);
}