CREATE TABLE click_events (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(12) NOT NULL,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_click_code_time ON click_events(short_code, clicked_at DESC);
