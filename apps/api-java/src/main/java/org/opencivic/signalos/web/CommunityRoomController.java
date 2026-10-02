package org.opencivic.signalos.web;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityRoomEventBroker;
import org.opencivic.signalos.service.CommunityRoomService;
import org.opencivic.signalos.web.dto.CommunityRoomDetailResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMentionInboxResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMessageResponse;
import org.opencivic.signalos.web.dto.CommunityRoomMuteResponse;
import org.opencivic.signalos.web.dto.CommunityRoomWorkspaceResponse;
import org.opencivic.signalos.web.dto.CreateCommunityRoomRequest;
import org.opencivic.signalos.web.dto.PostCommunityRoomMessageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/community/rooms")
public class CommunityRoomController {
    private final CommunityRoomService roomService;
    private final CommunityRoomEventBroker eventBroker;

    public CommunityRoomController(
        CommunityRoomService roomService,
        CommunityRoomEventBroker eventBroker
    ) {
        this.roomService = roomService;
        this.eventBroker = eventBroker;
    }

    @GetMapping("/workspace")
    public CommunityRoomWorkspaceResponse getWorkspace(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return roomService.getWorkspace(communityId, principal.getName());
    }

    @GetMapping("/{roomId}")
    public CommunityRoomDetailResponse getRoom(
        @PathVariable UUID roomId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return roomService.getRoom(communityId, roomId, principal.getName());
    }

    @PostMapping
    public CommunityRoomWorkspaceResponse createRoom(
        @Valid @RequestBody CreateCommunityRoomRequest request,
        Principal principal
    ) {
        roomService.createRoom(request, principal.getName());
        return roomService.getWorkspace(request.communityId(), principal.getName());
    }

    @PostMapping("/messages")
    public CommunityRoomMessageResponse postMessage(
        @Valid @RequestBody PostCommunityRoomMessageRequest request,
        Principal principal
    ) {
        return roomService.postMessage(request, principal.getName());
    }

    @PatchMapping("/{roomId}/mute")
    public CommunityRoomMuteResponse setMute(
        @PathVariable UUID roomId,
        @RequestParam UUID communityId,
        @RequestParam boolean muted,
        Principal principal
    ) {
        return roomService.setMute(communityId, roomId, muted, principal.getName());
    }

    @PatchMapping("/mentions/read")
    public CommunityRoomMentionInboxResponse markMentionsRead(
        @RequestParam UUID communityId,
        @RequestParam(required = false) UUID roomId,
        Principal principal
    ) {
        return roomService.markMentionsRead(communityId, roomId, principal.getName());
    }

    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
        @PathVariable UUID roomId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        roomService.getRoom(communityId, roomId, principal.getName());
        return eventBroker.subscribe(roomId);
    }
}