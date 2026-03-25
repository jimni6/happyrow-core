CREATE SCHEMA IF NOT EXISTS configuration;

DO $$ BEGIN
  CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

CREATE TABLE IF NOT EXISTS configuration.event (
  identifier UUID PRIMARY KEY,
  name        VARCHAR(256) NOT NULL,
  description TEXT NOT NULL,
  event_date  TIMESTAMP NOT NULL,
  creator     VARCHAR(256) NOT NULL,
  location    VARCHAR(256) NOT NULL,
  type        EVENT_TYPE NOT NULL,
  creation_date TIMESTAMP NOT NULL,
  update_date   TIMESTAMP NOT NULL,
  members     UUID[] NOT NULL
);

CREATE TABLE IF NOT EXISTS configuration.participant (
  id         UUID PRIMARY KEY,
  user_email VARCHAR(255) NOT NULL,
  user_name  VARCHAR(255) DEFAULT NULL,
  event_id   UUID NOT NULL REFERENCES configuration.event(identifier),
  status     VARCHAR(50) DEFAULT 'CONFIRMED',
  joined_at  TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_participant_user_event ON configuration.participant(user_email, event_id);
CREATE INDEX IF NOT EXISTS idx_participant_user ON configuration.participant(user_email);
CREATE INDEX IF NOT EXISTS idx_participant_event ON configuration.participant(event_id);

CREATE TABLE IF NOT EXISTS configuration.resource (
  id                 UUID PRIMARY KEY,
  name               VARCHAR(255) NOT NULL,
  category           VARCHAR(50) NOT NULL,
  suggested_quantity INTEGER DEFAULT 0,
  current_quantity   INTEGER DEFAULT 0,
  event_id           UUID NOT NULL REFERENCES configuration.event(identifier),
  version            INTEGER DEFAULT 0,
  created_at         TIMESTAMP NOT NULL,
  updated_at         TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_resource_event ON configuration.resource(event_id);

CREATE TABLE IF NOT EXISTS configuration.contribution (
  id             UUID PRIMARY KEY,
  participant_id UUID NOT NULL REFERENCES configuration.participant(id),
  resource_id    UUID NOT NULL REFERENCES configuration.resource(id),
  quantity       INTEGER NOT NULL CHECK (quantity > 0),
  created_at     TIMESTAMP NOT NULL,
  updated_at     TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_contribution_participant_resource ON configuration.contribution(participant_id, resource_id);
CREATE INDEX IF NOT EXISTS idx_contribution_participant ON configuration.contribution(participant_id);
CREATE INDEX IF NOT EXISTS idx_contribution_resource ON configuration.contribution(resource_id);
