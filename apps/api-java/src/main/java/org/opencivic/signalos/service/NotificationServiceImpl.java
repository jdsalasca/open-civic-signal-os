package org.opencivic.signalos.service;

import org.opencivic.signalos.domain.Notification;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final PrioritizationService prioritizationService;
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(PrioritizationService prioritizationService, NotificationRepository notificationRepository) {
        this.prioritizationService = prioritizationService;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void sendTop10Relay() {
        List<Signal> top10 = prioritizationService.getTopUnresolved(10);
        String summary = top10.stream()
                .map(s -> String.format("[%s] %s", s.getCategory(), s.getTitle()))
                .collect(Collectors.joining(", "));

        Notification n = new Notification(
            "EMAIL",
            "Top unresolved community issues: " + summary,
            "ALL_STAFF",
            LocalDateTime.now()
        );
        
        notificationRepository.save(n);
    }

    @Override
    public List<Notification> getRecentNotifications() {
        return notificationRepository.findTop50ByOrderBySentAtDesc();
    }
}
