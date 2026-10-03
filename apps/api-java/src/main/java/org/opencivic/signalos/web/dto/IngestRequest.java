package org.opencivic.signalos.web.dto;

import java.util.UUID;

public record IngestRequest(
    UUID communityId,
    String source,
    String fileName,
    String content,
    boolean commit
) {}