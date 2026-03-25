ALTER TABLE configuration.participant ADD COLUMN user_id UUID;
DROP INDEX IF EXISTS configuration.uq_participant_user_event;
DROP INDEX IF EXISTS configuration.idx_participant_user;
ALTER TABLE configuration.participant DROP COLUMN user_email;
CREATE UNIQUE INDEX uq_participant_user_event ON configuration.participant(user_id, event_id);
CREATE INDEX idx_participant_user ON configuration.participant(user_id);
