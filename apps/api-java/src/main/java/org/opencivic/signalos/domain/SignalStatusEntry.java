package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signal_status_history")
public class SignalStatusEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID signalId;

    @Column(nullable = false)
    private String statusFrom;

    @Column(nullable = false)
    private String statusTo;

    @Column(nullable = false)
    private String eventType = "STATUS_CHANGED";

    private String changedBy;

    private String assignedToUsername;
    
    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();

    public SignalStatusEntry() {}

    public SignalStatusEntry(UUID signalId, String statusFrom, String statusTo, String changedBy, String reason) {
        this(signalId, statusFrom, statusTo, "STATUS_CHANGED", changedBy, reason, null);
    }

    public SignalStatusEntry(
        UUID signalId,
        String statusFrom,
        String statusTo,
        String eventType,
        String changedBy,
        String reason,
        String assignedToUsername
    ) {
        this.signalId = signalId;
        this.statusFrom = statusFrom;
        this.statusTo = statusTo;
        this.eventType = eventType;
        this.changedBy = changedBy;
        this.reason = reason;
        this.assignedToUsername = assignedToUsername;
    }

    public UUID getId() { return id; }
    public UUID getSignalId() { return signalId; }
    public String getStatusFrom() { return statusFrom; }
    public String getStatusTo() { return statusTo; }
    public String getEventType() { return eventType; }
    public String getChangedBy() { return changedBy; }
    public String getAssignedToUsername() { return assignedToUsername; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
