ALTER TABLE short_urls
    ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    ADD CONSTRAINT chk_short_urls_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'));
