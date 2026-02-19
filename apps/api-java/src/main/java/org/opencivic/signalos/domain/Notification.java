package org.opencivic.signalos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private String channel;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    private String recipientGroup;
    
    private LocalDateTime sentAt;

    public Notification() {}

    public Notification(String channel, String message, String recipientGroup, LocalDateTime sentAt) {
        this.channel = channel;
        this.message = message;
        this.recipientGroup = recipientGroup;
        this.sentAt = sentAt;
    }

    public UUID getId() { return id; }
    public String getChannel() { return channel; }
    public String getMessage() { return message; }
    public String getRecipientGroup() { return recipientGroup; }
    public LocalDateTime getSentAt() { return sentAt; }
}
