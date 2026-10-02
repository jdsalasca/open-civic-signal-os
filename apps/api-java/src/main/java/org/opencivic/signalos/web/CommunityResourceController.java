package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.UUID;
import org.opencivic.signalos.service.CommunityResourceService;
import org.opencivic.signalos.web.dto.CommunityResourceBoardResponse;
import org.opencivic.signalos.web.dto.CommunityResourceBookingResponse;
import org.opencivic.signalos.web.dto.CreateCommunityResourceBookingRequest;
import org.opencivic.signalos.web.dto.CreateCommunityResourceRequest;
import org.opencivic.signalos.web.dto.DecideCommunityResourceBookingRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/resources")
public class CommunityResourceController {
    private final CommunityResourceService resourceService;

    public CommunityResourceController(CommunityResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/board")
    public CommunityResourceBoardResponse getBoard(
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return resourceService.getBoard(communityId, principal.getName());
    }

    @PostMapping
    public CommunityResourceBoardResponse createResource(
        @RequestBody CreateCommunityResourceRequest request,
        Principal principal
    ) {
        return resourceService.createResource(request, principal.getName());
    }

    @PostMapping("/bookings")
    public CommunityResourceBookingResponse requestBooking(
        @RequestBody CreateCommunityResourceBookingRequest request,
        Principal principal
    ) {
        return resourceService.requestBooking(request, principal.getName());
    }

    @PatchMapping("/bookings")
    public CommunityResourceBookingResponse decideBooking(
        @RequestBody DecideCommunityResourceBookingRequest request,
        Principal principal
    ) {
        return resourceService.decideBooking(request, principal.getName());
    }

    @DeleteMapping("/bookings/{bookingId}")
    public CommunityResourceBookingResponse cancelBooking(
        @PathVariable UUID bookingId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return resourceService.cancelBooking(communityId, bookingId, principal.getName());
    }

    @DeleteMapping("/{resourceId}")
    public CommunityResourceBoardResponse archiveResource(
        @PathVariable UUID resourceId,
        @RequestParam UUID communityId,
        Principal principal
    ) {
        return resourceService.archiveResource(communityId, resourceId, principal.getName());
    }
}