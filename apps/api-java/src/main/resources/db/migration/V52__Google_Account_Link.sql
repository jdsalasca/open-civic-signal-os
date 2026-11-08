-- V52: link a local account to a Google identity ("sub" claim).
-- Null is allowed and not unique-limited: every password-only account keeps it null.
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255);

CREATE UNIQUE INDEX users_google_sub_key ON users (google_sub);
