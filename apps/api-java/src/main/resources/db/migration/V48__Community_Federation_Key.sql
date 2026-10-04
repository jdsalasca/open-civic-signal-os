-- A stable, public key per community, so a peer can address a city across instances.
--
-- Community ids are instance-local: the open-data export URL embeds community.id, so a peer had to
-- already know a UUID minted by this deployment before it could ask for anything. Slugs are public
-- but mutable, and a federation key that changes under a peer breaks it silently.
--
-- Backfilled from the id's own hex rather than from a hash function, because this migration has to
-- run on PostgreSQL and on the H2 the tests use, and a portable expression is worth more here than a
-- digest. New communities get a random key from Community.assignFederationKey.
--
-- Unique, so a key cannot name two cities.

ALTER TABLE communities ADD COLUMN federation_key VARCHAR(64) NULL;

UPDATE communities
    SET federation_key = SUBSTRING(REPLACE(id::VARCHAR, '-', ''), 1, 24)
    WHERE federation_key IS NULL;

ALTER TABLE communities ALTER COLUMN federation_key SET NOT NULL;

CREATE UNIQUE INDEX idx_communities_federation_key ON communities (federation_key);