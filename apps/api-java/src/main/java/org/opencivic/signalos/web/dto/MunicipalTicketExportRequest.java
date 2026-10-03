package org.opencivic.signalos.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/**
 * Request for a municipal ticket export.
 *
 * <p>{@code categoryMap} is the community's own translation from platform categories to city
 * service codes. It is passed per request rather than stored, because the codes belong to one
 * city's helpdesk and a stored mapping would silently keep applying after the city renames a
 * service.
 */
public record MunicipalTicketExportRequest(
    @NotNull UUID communityId,
    Map<String, String> categoryMap,
    boolean includeUnmapped
) {}