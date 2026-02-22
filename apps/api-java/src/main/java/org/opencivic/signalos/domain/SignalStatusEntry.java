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

    private String changedBy;
    
    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();

    public SignalStatusEntry() {}

    public SignalStatusEntry(UUID signalId, String statusFrom, String statusTo, String changedBy, String reason) {
        this.signalId = signalId;
        this.statusFrom = statusFrom;
        this.statusTo = statusTo;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    public UUID getId() { return id; }
    public UUID getSignalId() { return signalId; }
    public String getStatusFrom() { return statusFrom; }
    public String getStatusTo() { return statusTo; }
    public String getChangedBy() { return changedBy; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
