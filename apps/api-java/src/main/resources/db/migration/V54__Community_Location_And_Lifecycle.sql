-- V54: coordinates for the nearest-city lookup and an active flag for empty neighbourhoods.

ALTER TABLE communities ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE communities ADD COLUMN longitude DOUBLE PRECISION;
ALTER TABLE communities ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
