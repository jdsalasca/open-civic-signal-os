ALTER TABLE users
    ADD COLUMN onboarding_completed_step_keys_csv TEXT;

ALTER TABLE users
    ADD COLUMN dismissed_guide_keys_csv TEXT;
