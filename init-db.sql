-- Database initialization script for HappyRow Core local development
-- This script runs automatically when the PostgreSQL container starts

-- Create the configuration schema
CREATE SCHEMA IF NOT EXISTS configuration;

-- Create the EVENT_TYPE enum
DO $$ BEGIN
  CREATE TYPE EVENT_TYPE AS ENUM ('PARTY', 'BIRTHDAY', 'DINER', 'SNACK');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

-- Grant necessary permissions to the user
GRANT ALL PRIVILEGES ON SCHEMA configuration TO happyrow_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA configuration TO happyrow_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA configuration TO happyrow_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA configuration GRANT ALL ON TABLES TO happyrow_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA configuration GRANT ALL ON SEQUENCES TO happyrow_user;

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log completion
SELECT 'Database initialization completed successfully' AS status;
