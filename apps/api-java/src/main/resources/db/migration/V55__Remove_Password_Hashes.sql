-- V55: production authentication is Google-only. No password hash is stored or accepted.
-- The column stays for schema compatibility, but every value becomes empty.

UPDATE users SET password = '';
