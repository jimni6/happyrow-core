ALTER TABLE configuration.event
  ALTER COLUMN creator TYPE UUID USING creator::uuid;
