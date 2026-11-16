package org.opencivic.signalos.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opencivic.signalos.service.UserNotificationService;
import org.opencivic.signalos.web.dto.UserNotificationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class UserNotificationController {
    private final UserNotificationService notificationService;

    public UserNotificationController(UserNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/inbox")
    public List<UserNotificationResponse> inbox(Principal principal) {
        return notificationService.inbox(principal.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("unread", notificationService.unreadCount(principal.getName()));
    }

    @PostMapping("/{notificationId}/read")
    public void markRead(@PathVariable UUID notificationId, Principal principal) {
        notificationService.markRead(notificationId, principal.getName());
    }

    @PostMapping("/read-all")
    public void markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
    }
}
