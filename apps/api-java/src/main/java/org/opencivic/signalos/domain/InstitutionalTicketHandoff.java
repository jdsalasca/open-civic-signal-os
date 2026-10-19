package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A report the platform handed to an institution, and what it knows about the outcome.
 *
 * <p>The platform cannot read a city helpdesk. Every status here is therefore either supplied by a
 * person or derived from our own signal lifecycle, and the entity is shaped so that is obvious:
 * there is no field that pretends to be a live view of the institution's queue.
 *
 * <p>{@code slaTargetDays} is captured at handoff rather than read from current settings. A
 * community tightening its SLA next month must not retroactively turn a past on-time handoff into a
 * late one.
 */
@Entity
@Table(name = "institutional_ticket_handoffs")
public class InstitutionalTicketHandoff {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private UUID signalId;

    @Column(nullable = false, length = 40)
    private String ticketRef;

    @Column(nullable = false, length = 80)
    private String externalCategoryCode;

    @Column(length = 120)
    private String externalTicketId;

    @Column(nullable = false)
    private UUID handedOffBy;

    @Column(nullable = false)
    private LocalDateTime handedOffAt = LocalDateTime.now();

    @Column(nullable = false)
    private int slaTargetDays;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime lastCheckedAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    public InstitutionalTicketHandoff() {}

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

    public UUID getSignalId() {
        return signalId;
    }

    public void setSignalId(UUID signalId) {
        this.signalId = signalId;
    }

    public String getTicketRef() {
        return ticketRef;
    }

    public void setTicketRef(String ticketRef) {
        this.ticketRef = ticketRef;
    }

    public String getExternalCategoryCode() {
        return externalCategoryCode;
    }

    public void setExternalCategoryCode(String externalCategoryCode) {
        this.externalCategoryCode = externalCategoryCode;
    }

    public String getExternalTicketId() {
        return externalTicketId;
    }

    public void setExternalTicketId(String externalTicketId) {
        this.externalTicketId = externalTicketId;
    }

    public UUID getHandedOffBy() {
        return handedOffBy;
    }

    public void setHandedOffBy(UUID handedOffBy) {
        this.handedOffBy = handedOffBy;
    }

    public LocalDateTime getHandedOffAt() {
        return handedOffAt;
    }

    public void setHandedOffAt(LocalDateTime handedOffAt) {
        this.handedOffAt = handedOffAt;
    }

    public int getSlaTargetDays() {
        return slaTargetDays;
    }

    public void setSlaTargetDays(int slaTargetDays) {
        this.slaTargetDays = slaTargetDays;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}