-- Add folder_path to media for organizing uploads into logical folders.
-- NULL / empty string means the root of the album (no sub-folder).
ALTER TABLE media ADD COLUMN IF NOT EXISTS folder_path VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_media_folder_path ON media (album_id, folder_path);
