package org.opencivic.signalos.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Pre-publication anonymization checklist for a community's open data.
 *
 * <p>This is the enforcement surface referenced by the AGENTS.md rule that public data gets
 * an anonymization step before publishing. A token cannot be minted while
 * {@code blockingFindings} is non-empty unless the caller explicitly acknowledges the
 * residual risk, which is recorded rather than implied.
 */
public record PublicDataAnonymizationChecklist(
    String communityId,
    LocalDateTime generatedAt,
    List<PublicDataAnonymizationFieldCheck> fields,
    List<String> blockingFindings,
    boolean publishable,
    List<String> categories
) {}