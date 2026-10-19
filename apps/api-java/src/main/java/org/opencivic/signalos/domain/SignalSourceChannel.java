package org.opencivic.signalos.domain;

/**
 * Where a civic signal entered the system. Kept as an explicit enum so an audit can
 * distinguish a citizen report from an institutional or automated update, per the
 * "distinguish citizen reports from institutional updates" rule.
 */
public enum SignalSourceChannel {
    WEB_FORM,
    CSV_IMPORT,
    CHAT_EXPORT,
    PUBLIC_API,
    INSTITUTIONAL_UPDATE
}