package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Notification;
import org.opencivic.signalos.domain.Signal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final PrioritizationService prioritizationService;
    private final List<Notification> history = new ArrayList<>();

    public NotificationServiceImpl(PrioritizationService prioritizationService) {
        this.prioritizationService = prioritizationService;
        // Mock some initial history
        history.add(new Notification(UUID.randomUUID(), "WHATSAPP", "Weekly Digest: 5 new infrastructure signals prioritized.", "Community Leaders", LocalDateTime.now().minusDays(2)));
    }

    @Override
    public List<Notification> getRecentNotifications() {
        return history;
    }

    @Override
    public void sendTop10Relay() {
        List<Signal> top10 = prioritizationService.getTopUnresolved(10);
        String summary = top10.stream()
                .map(s -> s.getTitle() + " (Score: " + Math.round(s.getPriorityScore()) + ")")
                .collect(Collectors.joining("\n"));

        Notification n = new Notification(
            UUID.randomUUID(),
            "TELEGRAM",
            "URGENT TOP 10:\n" + summary,
            "Public Works Taskforce",
            LocalDateTime.now()
        );
        history.add(n);
    }
}
