package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One decision an assembly recorded.
 *
 * <p>{@code subjectId} is nullable because an assembly can decide something that is not yet a
 * proposal. Forcing a link would make people create placeholder records to satisfy the schema, and a
 * placeholder in a decision log is worse than an honest "this was about something not in the system".
 *
 * <p>{@code rationale} is required. A decision nobody explained cannot be reviewed later, which is
 * the whole reason for recording it.
 */
@Entity
@Table(name = "assembly_decisions")
public class AssemblyDecision {

    public enum DecisionType {
        APPROVED,
        REJECTED,
        DEFERRED,
        NOTED
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID assemblyId;

    private UUID subjectId;

    @Column(length = 20)
    private String subjectType;

    @Column(nullable = false, length = 30)
    private String decision;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public AssemblyDecision() {}

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

    public DecisionType getDecision() {
        return DecisionType.valueOf(decision);
    }

    public void setDecision(DecisionType decision) {
        this.decision = decision.name();
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public UUID getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}