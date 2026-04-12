-- Analytics events table (missed from V1)
CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    album_id UUID,
    media_id UUID,
    photographer_id UUID,
    client_ip INET,
    user_agent TEXT,
    referer TEXT,
    country_code VARCHAR(2),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analytics_album_date        ON analytics_events(album_id, created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_photographer       ON analytics_events(photographer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type        ON analytics_events(event_type, created_at);
