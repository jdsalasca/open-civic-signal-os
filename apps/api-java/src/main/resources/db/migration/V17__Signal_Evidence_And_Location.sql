ALTER TABLE signals
    ADD COLUMN IF NOT EXISTS location_label TEXT;

CREATE TABLE IF NOT EXISTS signal_evidence_urls (
    signal_id UUID NOT NULL,
    evidence_url TEXT NOT NULL,
    CONSTRAINT fk_signal_evidence_signal
        FOREIGN KEY (signal_id) REFERENCES signals(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_signal_evidence_signal_id
    ON signal_evidence_urls(signal_id);
