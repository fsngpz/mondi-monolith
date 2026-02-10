/*
 * Create email_verification_tokens table for email verification
 */

-- Create email_verification_tokens table
CREATE TABLE email_verification_tokens
(
    id              BIGSERIAL
        CONSTRAINT email_verification_tokens_id_pk
            PRIMARY KEY,
    user_id         bigint
        CONSTRAINT email_verification_tokens_user_id_fk
            REFERENCES users
            ON UPDATE CASCADE ON DELETE CASCADE                        NOT NULL,
    token           text                                               NOT NULL,
    expires_at      timestamp WITH TIME ZONE                           NOT NULL,
    verified_at     timestamp WITH TIME ZONE,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text                                               NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text                                               NOT NULL,
    version         bigint                   DEFAULT 0                 NOT NULL
);

-- Create unique index on token for faster lookup and to prevent duplicates
CREATE UNIQUE INDEX email_verification_tokens_token_uindex ON email_verification_tokens (token);

-- Create index on user_id for faster lookup of user tokens
CREATE INDEX email_verification_tokens_user_id_index ON email_verification_tokens (user_id);

-- Create index on expires_at for faster cleanup of expired tokens
CREATE INDEX email_verification_tokens_expires_at_index ON email_verification_tokens (expires_at);

-- Create index on verified_at to quickly find unverified tokens
CREATE INDEX email_verification_tokens_verified_at_index ON email_verification_tokens (verified_at) WHERE verified_at IS NULL;

-- Create composite index for finding valid tokens (not verified and not expired)
CREATE INDEX email_verification_tokens_user_valid_index ON email_verification_tokens (user_id, expires_at, verified_at)
    WHERE verified_at IS NULL;
