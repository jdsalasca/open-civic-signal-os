package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "signal_id"})
})
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "signal_id", nullable = false)
    private UUID signalId;

    public Vote() {}

    public Vote(UUID userId, UUID signalId) {
        this.userId = userId;
        this.signalId = signalId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getSignalId() { return signalId; }
}
