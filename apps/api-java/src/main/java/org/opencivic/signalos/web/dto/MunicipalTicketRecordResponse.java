package org.opencivic.signalos.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One municipal ticket row, in the city helpdesk's shape.
 *
 * <p>Note what is absent: no author id, no account, no contact details. A ticket export leaves
 * the platform and lands in a procurement system with its own access rules, so the redacted
 * view is the only one that leaves.
 */
public record MunicipalTicketRecordResponse(
    String ticketRef,
    String externalCategoryCode,
    String title,
    String description,
    String location,
    String status,
    BigDecimal priorityScore,
    LocalDateTime reportedAt,
    String sourceChannel,
    String sourceRef,
    String platformUrl
) {}