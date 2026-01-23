/*
 * Add OAuth support to users table
 */

-- Create enum type for OAuth providers
CREATE TYPE oauth_provider as enum ('LOCAL', 'GOOGLE');

-- Make password nullable for OAuth users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Add OAuth provider column (default to LOCAL for existing users)
ALTER TABLE users ADD COLUMN provider oauth_provider DEFAULT 'LOCAL' NOT NULL;

-- Add OAuth provider ID column (unique identifier from OAuth provider)
ALTER TABLE users ADD COLUMN provider_id text;

-- Add unique constraint for provider + provider_id combination
CREATE UNIQUE INDEX users_provider_provider_id_uindex ON users (provider, provider_id) WHERE provider_id IS NOT NULL;

-- Add constraint to ensure password is not null when provider is LOCAL
ALTER TABLE users ADD CONSTRAINT check_password_for_local_provider
    CHECK (
        (provider = 'LOCAL' AND password IS NOT NULL) OR
        (provider != 'LOCAL' AND provider_id IS NOT NULL)
    );
