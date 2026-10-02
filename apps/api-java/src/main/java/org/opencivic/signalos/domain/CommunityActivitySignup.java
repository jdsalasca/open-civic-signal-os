package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_activity_signups")
public class CommunityActivitySignup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID activityId;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID volunteerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityActivitySignupStatus status = CommunityActivitySignupStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityActivityAttendanceStatus attendanceStatus = CommunityActivityAttendanceStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime cancelledAt;

    private LocalDateTime attendedAt;

    public UUID getId() { return id; }
    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }
    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }
    public UUID getVolunteerId() { return volunteerId; }
    public void setVolunteerId(UUID volunteerId) { this.volunteerId = volunteerId; }
    public CommunityActivitySignupStatus getStatus() { return status; }
    public void setStatus(CommunityActivitySignupStatus status) { this.status = status; }
    public CommunityActivityAttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(CommunityActivityAttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getAttendedAt() { return attendedAt; }
    public void setAttendedAt(LocalDateTime attendedAt) { this.attendedAt = attendedAt; }
    public boolean isConfirmed() { return status == CommunityActivitySignupStatus.CONFIRMED; }
}