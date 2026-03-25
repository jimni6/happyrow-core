CREATE TABLE IF NOT EXISTS configuration.app_user (
  id         UUID PRIMARY KEY,
  email      VARCHAR(255) NOT NULL,
  name       VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_user_email ON configuration.app_user(email);
