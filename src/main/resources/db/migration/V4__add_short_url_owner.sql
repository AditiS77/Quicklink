ALTER TABLE short_urls
    ADD COLUMN owner_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_short_urls_owner
        FOREIGN KEY (owner_id) REFERENCES app_users (id);

CREATE INDEX idx_short_urls_owner_id ON short_urls (owner_id);
