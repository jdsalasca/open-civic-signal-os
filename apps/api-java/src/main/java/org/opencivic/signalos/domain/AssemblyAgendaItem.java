package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One item in an assembly's running order.
 *
 * <p>{@code plannedMinutes} is required because a facilitator needs a number to pace against, and
 * "we will discuss it" is not a plan. The platform records the plan and reports whether the meeting
 * is running to it; it does not run the meeting.
 *
 * <p>{@code subjectId} is nullable for the same reason assembly decisions are: a townhall discusses
 * things that are not yet records, and forcing a link would make people create placeholders.
 */
@Entity
@Table(name = "assembly_agenda_items")
public class AssemblyAgendaItem {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID assemblyId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String title;

    private UUID subjectId;

    @Column(length = 20)
    private String subjectType;

    @Column(nullable = false)
    private int plannedMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AssemblyAgendaItem() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAssemblyId() {
        return assemblyId;
    }

    public void setAssemblyId(UUID assemblyId) {
        this.assemblyId = assemblyId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public void setPlannedMinutes(int plannedMinutes) {
        this.plannedMinutes = plannedMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}