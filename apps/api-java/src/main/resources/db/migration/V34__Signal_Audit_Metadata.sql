ALTER TABLE signals ADD COLUMN source_channel VARCHAR(30) NOT NULL DEFAULT 'WEB_FORM';

ALTER TABLE signals ADD COLUMN source_ref VARCHAR(180) NULL;

ALTER TABLE signals ADD COLUMN transformation_version VARCHAR(120) NOT NULL DEFAULT 'v1';

CREATE INDEX idx_signals_source_channel
    ON signals (source_channel);

CREATE INDEX idx_signals_transformation_version
    ON signals (transformation_version);