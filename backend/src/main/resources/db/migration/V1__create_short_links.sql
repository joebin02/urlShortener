CREATE TABLE short_links (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(12) NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    click_count BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_short_links_created_at ON short_links(created_at DESC);
