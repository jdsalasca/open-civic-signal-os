package org.opencivic.signalos.web.dto;

public record SignalAgingBucketResponse(
    String bucket,
    long count
) {}