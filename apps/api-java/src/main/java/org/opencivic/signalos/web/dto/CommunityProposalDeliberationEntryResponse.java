package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.opencivic.signalos.domain.CommunityProposalDeliberationType;

public record CommunityProposalDeliberationEntryResponse(
    UUID id,
    UUID proposalId,
    UUID authorId,
    String authorUsername,
    CommunityProposalDeliberationType entryType,
    String content,
    String supportingLink,
    boolean hidden,
    String moderationReason,
    String hiddenByUsername,
    LocalDateTime hiddenAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
