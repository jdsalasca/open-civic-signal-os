package org.opencivic.signalos.service;

public record EmailDeliveryResult(
    boolean delivered,
    String failureReason
) {
    public static EmailDeliveryResult success() {
        return new EmailDeliveryResult(true, null);
    }

    public static EmailDeliveryResult failed(String failureReason) {
        return new EmailDeliveryResult(false, failureReason);
    }
}
