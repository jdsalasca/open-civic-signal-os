-- The prepared digest, kept rather than recomposed at publish time.
--
-- "PREPARED" was a promise the code did not keep. The scheduler composed a digest, read its item
-- count and discarded the rest; publishing recomposed from live data. A coordinator could review
-- one digest on Monday and publish a different one on Wednesday, and the content hash a resident
-- received would correspond to nothing anybody had looked at.
--
-- The counts are columns rather than recomputed because they appear in the rendered body, and the
-- body is what residents receive. Anything not stored here cannot be reproduced from a review.

CREATE TABLE community_digest_preparations (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    week_key VARCHAR(10) NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    previous_week_key VARCHAR(10) NOT NULL,
    body TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    items_json TEXT NULL,
    item_count INTEGER NOT NULL,
    resolved_this_week INTEGER NOT NULL,
    rejected_this_week INTEGER NOT NULL,
    reported_this_week INTEGER NOT NULL,
    still_open_total INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL
);

-- One prepared digest per community per week. Also what makes a repeated scheduler fire a no-op
-- rather than a second artifact nobody reviewed.
CREATE UNIQUE INDEX idx_community_digest_preparations_community_week
    ON community_digest_preparations (community_id, week_key);