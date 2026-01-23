/*
 * Add Refresh Token support
 */

-- Create refresh_tokens table
CREATE TABLE refresh_tokens
(
    id              BIGSERIAL
        CONSTRAINT refresh_tokens_id_pk
            PRIMARY KEY,
    token           text                                               NOT NULL,
    user_id         bigint
        CONSTRAINT refresh_tokens_user_id_fk
            REFERENCES users
            ON UPDATE CASCADE ON DELETE CASCADE                        NOT NULL,
    expires_at      timestamp WITH TIME ZONE                           NOT NULL,
    revoked_at      timestamp WITH TIME ZONE,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text                                               NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text                                               NOT NULL,
    version         bigint                   DEFAULT 0                 NOT NULL
);

-- Create unique index on token
CREATE UNIQUE INDEX refresh_tokens_token_uindex ON refresh_tokens (token);

-- Create index on user_id for faster lookup
CREATE INDEX refresh_tokens_user_id_index ON refresh_tokens (user_id);

-- Create index on expires_at for cleanup queries
CREATE INDEX refresh_tokens_expires_at_index ON refresh_tokens (expires_at);
