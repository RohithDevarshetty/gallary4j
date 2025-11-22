-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create photographers table
CREATE TABLE IF NOT EXISTS photographers (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    studio_name VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    country_code VARCHAR(2),
    plan VARCHAR(50) DEFAULT 'trial',
    plan_expires_at TIMESTAMP WITH TIME ZONE,
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    storage_used_bytes BIGINT DEFAULT 0,
    storage_limit_bytes BIGINT DEFAULT 10737418240,
    albums_count INTEGER DEFAULT 0,
    albums_limit INTEGER DEFAULT 10,
    logo_url TEXT,
    watermark_url TEXT,
    custom_domain VARCHAR(255) UNIQUE,
    brand_colors JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    notification_preferences JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_photographer_email ON photographers(email);
CREATE INDEX idx_photographer_plan ON photographers(plan);

-- Create albums table
CREATE TABLE IF NOT EXISTS albums (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    photographer_id UUID NOT NULL REFERENCES photographers(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    event_date DATE,
    client_name VARCHAR(255),
    client_email VARCHAR(255),
    client_phone VARCHAR(20),
    password_hash VARCHAR(255),
    requires_password BOOLEAN DEFAULT false,
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    is_public BOOLEAN DEFAULT true,
    allow_downloads BOOLEAN DEFAULT true,
    allow_sharing BOOLEAN DEFAULT true,
    enable_selection BOOLEAN DEFAULT false,
    max_selections INTEGER,
    watermark_photos BOOLEAN DEFAULT false,
    view_count INTEGER DEFAULT 0,
    unique_visitors INTEGER DEFAULT 0,
    total_downloads INTEGER DEFAULT 0,
    last_viewed_at TIMESTAMP WITH TIME ZONE,
    media_count INTEGER DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    cover_photo_id UUID,
    category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    published_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_albums_photographer ON albums(photographer_id);
CREATE INDEX idx_albums_slug ON albums(slug);

-- Create album_tags table
CREATE TABLE IF NOT EXISTS album_tags (
    album_id UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (album_id, tag)
);

-- Create media table
CREATE TABLE IF NOT EXISTS media (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    album_id UUID NOT NULL REFERENCES albums(id) ON DELETE CASCADE,
    photographer_id UUID NOT NULL REFERENCES photographers(id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    original_filename VARCHAR(500),
    mime_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    original_url TEXT NOT NULL,
    optimized_url TEXT,
    thumbnail_url TEXT,
    preview_url TEXT,
    video_thumbnail_url TEXT,
    video_duration_seconds INTEGER,
    video_codec VARCHAR(50),
    width INTEGER,
    height INTEGER,
    aspect_ratio DECIMAL(5,2),
    orientation VARCHAR(20),
    camera_make VARCHAR(100),
    camera_model VARCHAR(100),
    lens_model VARCHAR(100),
    focal_length INTEGER,
    aperture DECIMAL(3,1),
    shutter_speed VARCHAR(20),
    iso INTEGER,
    taken_at TIMESTAMP WITH TIME ZONE,
    gps_latitude DECIMAL(10,8),
    gps_longitude DECIMAL(11,8),
    faces_detected INTEGER DEFAULT 0,
    face_data JSONB,
    sort_order INTEGER DEFAULT 0,
    is_cover BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,
    view_count INTEGER DEFAULT 0,
    download_count INTEGER DEFAULT 0,
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    processing_error TEXT,
    blur_hash VARCHAR(50),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_media_album ON media(album_id);
CREATE INDEX idx_media_processing ON media(processing_status);

-- Create media_auto_tags table
CREATE TABLE IF NOT EXISTS media_auto_tags (
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (media_id, tag)
);

-- Create media_color_palette table
CREATE TABLE IF NOT EXISTS media_color_palette (
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    color VARCHAR(7) NOT NULL,
    PRIMARY KEY (media_id, color)
);

-- Create selections table
CREATE TABLE IF NOT EXISTS selections (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    album_id UUID NOT NULL,
    media_id UUID NOT NULL,
    client_email VARCHAR(255),
    client_session_id VARCHAR(255),
    client_ip VARCHAR(50),
    selected_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    selection_type VARCHAR(50) DEFAULT 'favorite',
    notes TEXT,
    UNIQUE(album_id, media_id, client_email)
);

CREATE INDEX idx_selections_album ON selections(album_id);
CREATE INDEX idx_selections_client ON selections(client_email);

-- Create upload_sessions table
CREATE TABLE IF NOT EXISTS upload_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    photographer_id UUID NOT NULL REFERENCES photographers(id) ON DELETE CASCADE,
    album_id UUID NOT NULL,
    total_files INTEGER NOT NULL,
    uploaded_files INTEGER DEFAULT 0,
    total_bytes BIGINT NOT NULL,
    uploaded_bytes BIGINT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    error_message TEXT,
    client_type VARCHAR(50),
    client_version VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() + INTERVAL '24 hours',
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_upload_session_status ON upload_sessions(status, expires_at);
