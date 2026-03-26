CREATE TABLE IF NOT EXISTS configuration.event_invite (
  id            UUID PRIMARY KEY,
  token         VARCHAR(64)  NOT NULL,
  event_id      UUID         NOT NULL REFERENCES configuration.event(identifier),
  created_by    UUID         NOT NULL,
  status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  max_uses      INTEGER,
  current_uses  INTEGER      NOT NULL DEFAULT 0,
  created_at    TIMESTAMP    NOT NULL,
  expires_at    TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX idx_event_invite_token ON configuration.event_invite(token);
CREATE INDEX idx_event_invite_event_status ON configuration.event_invite(event_id, status);
