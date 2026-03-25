-- Existing rows may have email strings instead of UUIDs; assign generated UUIDs
UPDATE configuration.event
SET creator = gen_random_uuid()::text
WHERE creator !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

ALTER TABLE configuration.event
  ALTER COLUMN creator TYPE UUID USING creator::uuid;
