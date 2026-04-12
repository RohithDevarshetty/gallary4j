-- Backfill slugs for any albums that were created without one.
-- Generates: lowercase-title-with-dashes + first 8 chars of the album UUID.
UPDATE albums
SET slug = LOWER(REGEXP_REPLACE(title, '[^a-zA-Z0-9]+', '-', 'g'))
           || '-' || LEFT(id::text, 8)
WHERE slug IS NULL OR slug = '';
