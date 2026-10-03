package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityMembership;
import org.opencivic.signalos.domain.CommunityPermissionScope;
import org.opencivic.signalos.domain.CommunityProjectBoard;
import org.opencivic.signalos.domain.CommunityRoom;
import org.opencivic.signalos.domain.CommunityRoomMention;
import org.opencivic.signalos.domain.CommunityRoomMessage;
import org.opencivic.signalos.domain.CommunityRoomMute;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.CommunityMembershipRepository;
import org.opencivic.signalos.repository.CommunityProjectBoardRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.CommunityRoomMentionRepository;
import org.opencivic.signalos.repository.CommunityRoomMessageRepository;
import org.opencivic.signalos.repository.CommunityRoomMuteRepository;
import org.opencivic.signalos.repository.CommunityRoomRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.CommunityRoomDetailResponse;
import org.opencivic.signalos.web.dto.CommunityRoomEventResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMentionInboxResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMentionResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMessageResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMuteResponse;
import org.opencivic.signalos.web.dto.CommunityRoomSummaryResponse;
import org.opencivic.signalos.web.dto.CommunityRoomWorkspaceResponse;
import org.opencivic.signalos.web.dto.CreateCommunityRoomRequest;
import org.opencivic.signalos.web.dto.PostCommunityRoomMessageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityRoomService {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9._-]{2,40})");

    private final CommunityAccessService communityAccessService;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final CommunityProjectBoardRepository projectBoardRepository;
    private final UserRepository userRepository;
    private final CommunityRoomRepository roomRepository;
    private final CommunityRoomMessageRepository messageRepository;
    private final CommunityRoomMentionRepository mentionRepository;
    private final CommunityRoomMuteRepository muteRepository;
    private final CommunityRoomEventBroker eventBroker;

    public CommunityRoomService(
        CommunityAccessService communityAccessService,
        CommunityRepository communityRepository,
        CommunityMembershipRepository membershipRepository,
        CommunityProjectBoardRepository projectBoardRepository,
        UserRepository userRepository,
        CommunityRoomRepository roomRepository,
        CommunityRoomMessageRepository messageRepository,
        CommunityRoomMentionRepository mentionRepository,
        CommunityRoomMuteRepository muteRepository,
        CommunityRoomEventBroker eventBroker
    ) {
        this.communityAccessService = communityAccessService;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.projectBoardRepository = projectBoardRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.mentionRepository = mentionRepository;
        this.muteRepository = muteRepository;
        this.eventBroker = eventBroker;
    }

    @Transactional(readOnly = true)
    public CommunityRoomWorkspaceResponse getWorkspace(UUID communityId, String username, Integer limit) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        Community community = getCommunity(communityId);
        int max = CommunityListLimits.resolveLimit(limit);

        List<CommunityRoom> rooms = roomRepository.findByCommunityIdAndArchivedFalseOrderByCreatedAtDesc(communityId);
        Map<UUID, CommunityRoomMute> mutesByRoom = mutesFor(user.getId(), rooms.stream().map(CommunityRoom::getId).toList());

        List<CommunityRoomSummaryResponse> summaries = rooms.stream()
            .limit(max)
            .map(room -> toSummary(room, user.getId(), mutesByRoom))
            .sorted(Comparator.comparing(CommunityRoomSummaryResponse::lastActivityAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        return new CommunityRoomWorkspaceResponse(
            community.getId(),
            community.getName(),
            mentionableUsernames(communityId),
            summaries,
            buildInbox(communityId, user.getId())
        );
    }

    @Transactional(readOnly = true)
    public CommunityRoomDetailResponse getRoom(UUID communityId, UUID roomId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        CommunityRoom room = requireRoom(communityId, roomId);

        List<CommunityRoomMessage> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
        List<UUID> messageIds = messages.stream().map(CommunityRoomMessage::getId).toList();
        Map<UUID, List<CommunityRoomMention>> mentionsByMessage = mentionsByMessage(messageIds);

        List<CommunityRoomMessageResponse> payload = messages.stream()
            .map(message -> {
                List<CommunityRoomMention> mentions = mentionsByMessage.getOrDefault(message.getId(), List.of());
                return new CommunityRoomMessageResponse(
                    message.getId(),
                    roomId,
                    message.getAuthorId(),
                    displayNameOf(message.getAuthorId()),
                    message.getBody(),
                    message.getCreatedAt(),
                    mentions.stream().map(CommunityRoomMention::getMentionedUserId).distinct().toList(),
                    mentions.stream().anyMatch(mention -> mention.getMentionedUserId().equals(user.getId()))
                );
            })
            .toList();

        CommunityRoomMute mute = muteRepository.findByRoomIdAndUserId(roomId, user.getId()).orElse(null);
        return new CommunityRoomDetailResponse(
            room.getId(),
            room.getCommunityId(),
            room.getProjectBoardId(),
            room.getName(),
            room.getTopic(),
            room.getCreatedBy(),
            room.getCreatedAt(),
            room.isArchived(),
            mute != null,
            mute == null ? null : mute.getMutedAt(),
            messages.size(),
            mentionRepository.countByRoomIdAndMentionedUserIdAndReadAtIsNull(roomId, user.getId()),
            payload
        );
    }

    @Transactional
    public CommunityRoomSummaryResponse createRoom(CreateCommunityRoomRequest request, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.MANAGE_ROOMS);
        getCommunity(request.communityId());

        if (request.projectBoardId() != null) {
            CommunityProjectBoard board = projectBoardRepository.findById(request.projectBoardId())
                .orElseThrow(() -> new ResourceNotFoundException("Project board not found: " + request.projectBoardId()));
            if (!board.getCommunityId().equals(request.communityId())) {
                throw new ResourceNotFoundException("Project board does not belong to community " + request.communityId());
            }
        }

        CommunityRoom room = new CommunityRoom();
        room.setCommunityId(request.communityId());
        room.setProjectBoardId(request.projectBoardId());
        room.setName(request.name().trim());
        room.setTopic(request.topic().trim());
        room.setCreatedBy(user.getId());
        room = roomRepository.save(room);

        eventBroker.publish(new CommunityRoomEventResponse(
            "room-created", room.getId(), room.getCommunityId(), null, user.getId(), room.getCreatedAt()
        ));
        return toSummary(room, user.getId(), Map.of());
    }

    @Transactional
    public CommunityRoomMessageResponse postMessage(PostCommunityRoomMessageRequest request, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireScope(user.getId(), request.communityId(), CommunityPermissionScope.POST_ROOM_MESSAGE);
        CommunityRoom room = requireRoom(request.communityId(), request.roomId());

        CommunityRoomMessage message = new CommunityRoomMessage();
        message.setRoomId(room.getId());
        message.setCommunityId(room.getCommunityId());
        message.setAuthorId(user.getId());
        message.setBody(request.body().trim());
        message = messageRepository.save(message);

        List<CommunityRoomMention> mentions = createMentions(room, message, user.getId());
        mentionRepository.saveAll(mentions);

        eventBroker.publish(new CommunityRoomEventResponse(
            "room-message",
            room.getId(),
            room.getCommunityId(),
            message.getId(),
            user.getId(),
            message.getCreatedAt()
        ));

        return new CommunityRoomMessageResponse(
            message.getId(),
            room.getId(),
            user.getId(),
            displayNameOf(user.getId()),
            message.getBody(),
            message.getCreatedAt(),
            mentions.stream().map(CommunityRoomMention::getMentionedUserId).distinct().toList(),
            false
        );
    }

    @Transactional
    public CommunityRoomMuteResponse setMute(UUID communityId, UUID roomId, boolean muted, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        CommunityRoom room = requireRoom(communityId, roomId);

        CommunityRoomMute existing = muteRepository.findByRoomIdAndUserId(roomId, user.getId()).orElse(null);
        if (muted && existing == null) {
            CommunityRoomMute mute = new CommunityRoomMute();
            mute.setRoomId(roomId);
            mute.setCommunityId(room.getCommunityId());
            mute.setUserId(user.getId());
            existing = muteRepository.save(mute);
        } else if (!muted && existing != null) {
            muteRepository.delete(existing);
            existing = null;
        }

        return new CommunityRoomMuteResponse(roomId, communityId, existing != null, existing == null ? null : existing.getMutedAt());
    }

    @Transactional
    public CommunityRoomMentionInboxResponse markMentionsRead(UUID communityId, UUID roomId, String username) {
        User user = communityAccessService.getCurrentUser(username);
        communityAccessService.requireMembership(user.getId(), communityId);
        if (roomId != null) {
            requireRoom(communityId, roomId);
        }

        List<CommunityRoomMention> pending = roomId == null
            ? mentionRepository.findByMentionedUserIdAndCommunityIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId(), communityId)
            : mentionRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .filter(mention -> mention.getMentionedUserId().equals(user.getId()) && mention.getReadAt() == null)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        pending.forEach(mention -> mention.setReadAt(now));
        mentionRepository.saveAll(pending);

        return buildInbox(communityId, user.getId());
    }

    private CommunityRoomMentionInboxResponse buildInbox(UUID communityId, UUID userId) {
        List<CommunityRoomMention> pending =
            mentionRepository.findByMentionedUserIdAndCommunityIdAndReadAtIsNullOrderByCreatedAtDesc(userId, communityId);
        Map<UUID, CommunityRoomMention> dedupedByMessage = new LinkedHashMap<>();
        pending.forEach(mention -> dedupedByMessage.putIfAbsent(mention.getMessageId(), mention));

        List<CommunityRoomMentionResponse> items = dedupedByMessage.values().stream().limit(20).map(mention -> {
            CommunityRoom room = roomRepository.findById(mention.getRoomId()).orElse(null);
            CommunityRoomMessage message = messageRepository.findById(mention.getMessageId()).orElse(null);
            return new CommunityRoomMentionResponse(
                mention.getId(),
                mention.getRoomId(),
                room == null ? "" : room.getName(),
                mention.getMessageId(),
                message == null ? "" : message.getBody(),
                mention.getMentionedUserId(),
                message == null ? null : message.getAuthorId(),
                message == null ? "" : displayNameOf(message.getAuthorId()),
                mention.getCreatedAt(),
                mention.getReadAt()
            );
        }).toList();

        return new CommunityRoomMentionInboxResponse(communityId, pending.size(), items);
    }

    private List<CommunityRoomMention> createMentions(CommunityRoom room, CommunityRoomMessage message, UUID authorId) {
        List<String> handles = extractMentionHandles(message.getBody());
        if (handles.isEmpty()) {
            return List.of();
        }
        Map<String, UUID> memberIdsByUsername = memberIdsByUsername(room.getCommunityId());
        List<CommunityRoomMention> mentions = new ArrayList<>();
        for (String handle : handles) {
            UUID mentionedId = memberIdsByUsername.get(handle);
            if (mentionedId == null || mentionedId.equals(authorId)) {
                continue;
            }
            CommunityRoomMention mention = new CommunityRoomMention();
            mention.setMessageId(message.getId());
            mention.setRoomId(room.getId());
            mention.setCommunityId(room.getCommunityId());
            mention.setMentionedUserId(mentionedId);
            mentions.add(mention);
        }
        return mentions;
    }

    static List<String> extractMentionHandles(String body) {
        List<String> handles = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(body);
        while (matcher.find()) {
            String handle = matcher.group(1).toLowerCase(Locale.ROOT);
            if (!handles.contains(handle)) {
                handles.add(handle);
            }
        }
        return handles;
    }

    private List<String> mentionableUsernames(UUID communityId) {
        return memberIdsByUsername(communityId).keySet().stream().sorted().toList();
    }

    private Map<String, UUID> memberIdsByUsername(UUID communityId) {
        Map<String, UUID> result = new LinkedHashMap<>();
        for (CommunityMembership membership : membershipRepository.findByCommunityId(communityId)) {
            userRepository.findById(membership.getUserId()).ifPresent(user -> {
                if (user.getUsername() != null && !user.getUsername().isBlank()) {
                    result.put(user.getUsername().toLowerCase(Locale.ROOT), user.getId());
                }
            });
        }
        return result;
    }

    private Map<UUID, List<CommunityRoomMention>> mentionsByMessage(List<UUID> messageIds) {
        Map<UUID, List<CommunityRoomMention>> result = new LinkedHashMap<>();
        for (UUID messageId : messageIds) {
            result.put(messageId, mentionRepository.findByMessageId(messageId));
        }
        return result;
    }

    private Map<UUID, CommunityRoomMute> mutesFor(UUID userId, List<UUID> roomIds) {
        Map<UUID, CommunityRoomMute> result = new LinkedHashMap<>();
        if (roomIds.isEmpty()) {
            return result;
        }
        for (CommunityRoomMute mute : muteRepository.findByRoomIdInAndUserId(roomIds, userId)) {
            result.put(mute.getRoomId(), mute);
        }
        return result;
    }

    private CommunityRoomSummaryResponse toSummary(CommunityRoom room, UUID userId, Map<UUID, CommunityRoomMute> mutesByRoom) {
        List<CommunityRoomMessage> messages = messageRepository.findByRoomIdOrderByCreatedAtDesc(room.getId());
        CommunityRoomMute mute = mutesByRoom.get(room.getId());
        return new CommunityRoomSummaryResponse(
            room.getId(),
            room.getCommunityId(),
            room.getProjectBoardId(),
            room.getName(),
            room.getTopic(),
            room.getCreatedBy(),
            room.getCreatedAt(),
            room.isArchived(),
            messages.size(),
            mentionRepository.countByRoomIdAndMentionedUserIdAndReadAtIsNull(room.getId(), userId),
            mute != null,
            messages.isEmpty() ? room.getCreatedAt() : messages.get(0).getCreatedAt()
        );
    }

    private String displayNameOf(UUID userId) {
        return userRepository.findById(userId)
            .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername())
            .orElse("unknown");
    }

    private CommunityRoom requireRoom(UUID communityId, UUID roomId) {
        CommunityRoom room = roomRepository.findById(roomId)
            .orElseThrow(() -> new ResourceNotFoundException("Community room not found: " + roomId));
        if (!room.getCommunityId().equals(communityId)) {
            throw new ResourceNotFoundException("Community room not found: " + roomId);
        }
        return room;
    }

    private Community getCommunity(UUID communityId) {
        return communityRepository.findById(communityId)
            .orElseThrow(() -> new ResourceNotFoundException("Community not found: " + communityId));
    }
}