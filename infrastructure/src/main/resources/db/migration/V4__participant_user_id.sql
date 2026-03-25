ALTER TABLE configuration.participant ADD COLUMN user_id UUID;

-- Existing rows need a user_id; assign generated UUIDs
UPDATE configuration.participant SET user_id = gen_random_uuid() WHERE user_id IS NULL;

DROP INDEX IF EXISTS configuration.uq_participant_user_event;
DROP INDEX IF EXISTS configuration.idx_participant_user;
ALTER TABLE configuration.participant DROP COLUMN user_email;
CREATE UNIQUE INDEX uq_participant_user_event ON configuration.participant(user_id, event_id);
CREATE INDEX idx_participant_user ON configuration.participant(user_id);
