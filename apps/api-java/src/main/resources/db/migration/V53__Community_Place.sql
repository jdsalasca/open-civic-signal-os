-- V53: city / locality / neighborhood for a community, plus a normalized key so the same
-- place cannot be registered twice under different casing, accents or spacing.
-- Nullable on purpose: existing communities and non-geographic hubs keep place_key null.

ALTER TABLE communities ADD COLUMN city VARCHAR(120);
ALTER TABLE communities ADD COLUMN locality VARCHAR(120);
ALTER TABLE communities ADD COLUMN neighborhood VARCHAR(120);
ALTER TABLE communities ADD COLUMN place_key VARCHAR(400);

CREATE UNIQUE INDEX communities_place_key_key ON communities (place_key);
