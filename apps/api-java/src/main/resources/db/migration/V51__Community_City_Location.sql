-- V51__Community_City_Location.sql
-- Where a community is, so people can find and join their city from a directory.
-- Nullable on purpose: not every community is a municipality.

ALTER TABLE communities ADD COLUMN country VARCHAR(120);
ALTER TABLE communities ADD COLUMN region VARCHAR(120);
