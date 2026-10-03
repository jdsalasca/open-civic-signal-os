package org.opencivic.signalos.domain;

/**
 * Ingest channels that carry an external export. Each maps to a SignalSourceChannel on
 * the persisted signal, so an imported record is traceable to the export it came from.
 */
public enum IngestSource {
    CSV_EXPORT,
    WHATSAPP_EXPORT,
    TELEGRAM_EXPORT
}