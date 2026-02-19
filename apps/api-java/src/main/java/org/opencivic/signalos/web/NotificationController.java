package org.opencivic.signalos.web;

import org.opencivic.signalos.domain.Notification;
import org.opencivic.signalos.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/recent")
    public List<Notification> getRecent() {
        return notificationService.getRecentNotifications();
    }

    @PostMapping("/relay/top-10")
    public void sendRelay() {
        notificationService.sendTop10Relay();
    }
}
