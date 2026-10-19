package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One proposal's place in a budget envelope.
 *
 * <p>{@code amountMinor} is null when the proposal's free-text cost could not be read as an amount.
 * Null rather than a guess: a budget that silently invented a number would produce an allocation
 * that looks decided and is not. {@code rawCostText} is kept so a reader can see what the parser was
 * given when it failed.
 */
@Entity
@Table(name = "participatory_budget_allocations")
public class ParticipatoryBudgetAllocation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID budgetId;

    @Column(nullable = false)
    private UUID proposalId;

    private Long amountMinor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawCostText;

    @Column(nullable = false)
    private boolean included = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ParticipatoryBudgetAllocation() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(UUID budgetId) {
        this.budgetId = budgetId;
    }

    public UUID getProposalId() {
        return proposalId;
    }

    public void setProposalId(UUID proposalId) {
        this.proposalId = proposalId;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public void setAmountMinor(Long amountMinor) {
        this.amountMinor = amountMinor;
    }

    public String getRawCostText() {
        return rawCostText;
    }

    public void setRawCostText(String rawCostText) {
        this.rawCostText = rawCostText;
    }

    public boolean isIncluded() {
        return included;
    }

    public void setIncluded(boolean included) {
        this.included = included;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}