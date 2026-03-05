-- V4__Optimize_Signals_Table.sql
-- Escalability improvements for signal feeds

CREATE INDEX idx_signals_author_id ON signals(author_id);
CREATE INDEX idx_signals_priority_score ON signals(priority_score DESC);
CREATE INDEX idx_signals_status ON signals(status);
