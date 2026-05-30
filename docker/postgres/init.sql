-- Runs once on first container start (before the app / Flyway).
-- The database/user are created from POSTGRES_* env vars; this just adds extensions.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
