package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One unresolved case with its age and derived SLA risk. Risk is computed backend-side so
 * the dashboard and any public view agree on what "at risk" means.
 */
public record SignalAgingItemResponse(
    UUID id,
    String title,
    String category,
    String status,
    double priorityScore,
    long ageDays,
    long slaTargetDays,
    String slaRisk,
    long daysOverTarget,
    LocalDateTime createdAt
) {}