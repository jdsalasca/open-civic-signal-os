package org.opencivic.signalos.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.opencivic.signalos.domain.Signal;
import org.opencivic.signalos.domain.SignalConfirmation;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.exception.ResourceNotFoundException;
import org.opencivic.signalos.repository.SignalConfirmationRepository;
import org.opencivic.signalos.repository.SignalRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.opencivic.signalos.web.dto.SignalConfirmationStateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignalConfirmationService {
    private final SignalConfirmationRepository confirmationRepository;
    private final SignalRepository signalRepository;
    private final UserRepository userRepository;
    private final ContentFollowService followService;
    private final UserNotificationService notificationService;

    public SignalConfirmationService(
        SignalConfirmationRepository confirmationRepository,
        SignalRepository signalRepository,
        UserRepository userRepository,
        ContentFollowService followService,
        UserNotificationService notificationService
    ) {
        this.confirmationRepository = confirmationRepository;
        this.signalRepository = signalRepository;
        this.userRepository = userRepository;
        this.followService = followService;
        this.notificationService = notificationService;
    }

    @Transactional
    public SignalConfirmationStateResponse confirm(UUID signalId, String note, String username) {
        User user = requireUser(username);
        Signal signal = signalRepository.findById(signalId)
            .orElseThrow(() -> new ResourceNotFoundException("Signal not found: " + signalId));
        if (confirmationRepository.findBySignalIdAndUserId(signalId, user.getId()).isPresent()) {
            return state(signalId, username);
        }
        SignalConfirmation confirmation = new SignalConfirmation();
        confirmation.setSignalId(signalId);
        confirmation.setUserId(user.getId());
        confirmation.setNote(trimNote(note));
        confirmation.setCreatedAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);

        Set<UUID> recipients = new HashSet<>();
        if (signal.getAuthorId() != null) {
            recipients.add(signal.getAuthorId());
        }
        recipients.addAll(followService.followerIds("SIGNAL", signalId));
        for (UUID recipient : recipients) {
            notificationService.notifyUser(
                recipient,
                user.getId(),
                user.getUsername(),
                "SIGNAL_CONFIRMATION",
                "SIGNAL",
                signalId,
                signal.getTitle(),
                signal.getCommunityId()
            );
        }
        return state(signalId, username);
    }

    @Transactional
    public SignalConfirmationStateResponse unconfirm(UUID signalId, String username) {
        User user = requireUser(username);
        confirmationRepository.deleteBySignalIdAndUserId(signalId, user.getId());
        return state(signalId, username);
    }

    @Transactional(readOnly = true)
    public SignalConfirmationStateResponse state(UUID signalId, String username) {
        User user = requireUser(username);
        return new SignalConfirmationStateResponse(
            signalId,
            confirmationRepository.countBySignalId(signalId),
            confirmationRepository.findBySignalIdAndUserId(signalId, user.getId()).isPresent()
        );
    }

    @Transactional(readOnly = true)
    public Map<UUID, Long> countsFor(List<UUID> signalIds) {
        if (signalIds.isEmpty()) {
            return Map.of();
        }
        return confirmationRepository.findBySignalIdIn(signalIds).stream()
            .collect(Collectors.groupingBy(SignalConfirmation::getSignalId, Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Set<UUID> confirmedByViewer(UUID userId, List<UUID> signalIds) {
        if (signalIds.isEmpty()) {
            return Set.of();
        }
        return confirmationRepository.findByUserIdAndSignalIdIn(userId, signalIds).stream()
            .map(SignalConfirmation::getSignalId)
            .collect(Collectors.toSet());
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String trimNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String clean = note.trim();
        return clean.length() <= 500 ? clean : clean.substring(0, 500);
    }
}
