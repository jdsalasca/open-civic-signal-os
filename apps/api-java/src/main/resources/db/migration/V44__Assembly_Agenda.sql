CREATE TABLE assembly_agenda_items (
    id UUID PRIMARY KEY,
    assembly_id UUID NOT NULL REFERENCES community_assemblies(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    -- What the item is about, when it maps to something in the system. Nullable for the same reason
    -- assembly decisions are: a townhall discusses things that are not yet records.
    subject_id UUID NULL,
    subject_type VARCHAR(20) NULL,
    -- Planned minutes. A facilitator needs a number to pace against, and "we will discuss it" is not
    -- a plan.
    planned_minutes INTEGER NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- One position per assembly. Two items at position 3 means the order is undefined, and an agenda
-- whose order is undefined is not an agenda.
CREATE UNIQUE INDEX idx_agenda_items_position
    ON assembly_agenda_items (assembly_id, position);

CREATE INDEX idx_agenda_items_assembly
    ON assembly_agenda_items (assembly_id, position ASC);

COMMENT ON COLUMN assembly_agenda_items.planned_minutes IS
    'Planned duration. The facilitator paces against this; the platform does not enforce it.';
COMMENT ON TABLE assembly_agenda_items IS
    'The running order for an assembly. The platform records the plan and reports whether the meeting is running to it; it does not run the meeting.';