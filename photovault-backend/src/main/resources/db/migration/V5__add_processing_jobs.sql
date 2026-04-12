-- Processing jobs table — one row per media file, tracks the full lifecycle
-- from upload through Kafka processing to completion or failure.
CREATE TABLE IF NOT EXISTS processing_jobs (
    id              UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    media_id        UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    session_id      UUID REFERENCES upload_sessions(id) ON DELETE SET NULL,
    photographer_id UUID NOT NULL REFERENCES photographers(id) ON DELETE CASCADE,
    album_id        UUID NOT NULL,

    -- Job classification
    job_type        VARCHAR(20) NOT NULL,  -- IMAGE, VIDEO, THUMBNAIL
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED',  -- QUEUED, PROCESSING, COMPLETED, FAILED, RETRYING

    -- Retry tracking
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    max_attempts    INTEGER NOT NULL DEFAULT 3,

    -- Timing
    queued_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,

    -- Error info (last failure message)
    error_message   TEXT,

    -- Kafka correlation
    kafka_topic     VARCHAR(100),
    kafka_partition INTEGER,
    kafka_offset    BIGINT
);

CREATE INDEX idx_pjob_media        ON processing_jobs(media_id);
CREATE INDEX idx_pjob_session      ON processing_jobs(session_id);
CREATE INDEX idx_pjob_photographer ON processing_jobs(photographer_id);
CREATE INDEX idx_pjob_status       ON processing_jobs(status, queued_at);
CREATE INDEX idx_pjob_album        ON processing_jobs(album_id, status);
