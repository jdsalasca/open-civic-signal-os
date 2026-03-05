ALTER TABLE users
ADD COLUMN IF NOT EXISTS interface_mode VARCHAR(20) NOT NULL DEFAULT 'SIMPLE';

UPDATE users
SET interface_mode = 'SIMPLE'
WHERE interface_mode IS NULL;
