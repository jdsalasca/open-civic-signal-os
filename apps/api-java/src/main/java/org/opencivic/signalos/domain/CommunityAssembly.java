package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A community assembly: what was deliberated, over which evidence, and what was decided.
 *
 * <p>Deliberately not the official minutes. The platform records what happened in the room; the
 * authoritative record remains whatever the community is legally required to keep. A platform that
 * presented its own record as the minutes would be claiming an authority it does not have.
 *
 * <p>{@code snapshotId} is nullable because an assembly can be scheduled before its evidence is
 * captured. Forcing a snapshot at creation would make people create a placeholder one, and a
 * placeholder in an evidence chain is worse than an honest gap.
 */
@Entity
@Table(name = "community_assemblies")
public class CommunityAssembly {

    public enum Status {
        SCHEDULED,
        OPEN,
        CLOSED,
        CANCELLED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime scheduledFor;

    private String location;

    private UUID snapshotId;

    @Column(nullable = false)
    private UUID convenedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String status = Status.SCHEDULED.name();

    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Column(columnDefinition = "TEXT")
    private String minutes;

    public CommunityAssembly() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public UUID getConvenedBy() {
        return convenedBy;
    }

    public void setConvenedBy(UUID convenedBy) {
        this.convenedBy = convenedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Status getStatus() {
        return Status.valueOf(status);
    }

    public void setStatus(Status status) {
        this.status = status.name();
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getMinutes() {
        return minutes;
    }

    public void setMinutes(String minutes) {
        this.minutes = minutes;
    }
}