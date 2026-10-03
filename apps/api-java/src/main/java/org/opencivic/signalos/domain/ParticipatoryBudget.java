package org.opencivic.signalos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A community budget envelope for a participatory exercise.
 *
 * <p>The platform does not hold money. It records what a community decided to allocate and reports
 * whether the arithmetic fits, which is a different and more useful thing than a payment system.
 *
 * <p>{@code totalBudgetMinor} is a long in minor units. Money in a double is a rounding bug waiting
 * for a council meeting to expose it.
 */
@Entity
@Table(name = "participatory_budgets")
public class ParticipatoryBudget {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID communityId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long totalBudgetMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime closedAt;

    public ParticipatoryBudget() {}

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalBudgetMinor() {
        return totalBudgetMinor;
    }

    public void setTotalBudgetMinor(long totalBudgetMinor) {
        this.totalBudgetMinor = totalBudgetMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}