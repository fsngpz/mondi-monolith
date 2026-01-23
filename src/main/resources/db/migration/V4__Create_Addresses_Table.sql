/*
 * Create addresses table for user addresses
 */

-- Create enum type for address tags
CREATE TYPE address_tag as enum ('HOME', 'OFFICE', 'OTHER');

-- Create addresses table
CREATE TABLE addresses
(
    id              BIGSERIAL
        CONSTRAINT addresses_id_pk
            PRIMARY KEY,
    user_id         bigint
        CONSTRAINT addresses_user_id_fk
            REFERENCES users
            ON UPDATE CASCADE ON DELETE CASCADE                        NOT NULL,
    street          text                                               NOT NULL,
    city            text                                               NOT NULL,
    state           text,
    postal_code     text,
    country         text                                               NOT NULL,
    tag             address_tag                      DEFAULT 'HOME'    NOT NULL,
    is_main         boolean                          DEFAULT false     NOT NULL,
    label           text,
    notes           text,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text                                               NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text                                               NOT NULL,
    version         bigint                   DEFAULT 0                 NOT NULL
);

-- Create index on user_id for faster lookup
CREATE INDEX addresses_user_id_index ON addresses (user_id);

-- Create index on is_main for faster lookup of main addresses
CREATE INDEX addresses_is_main_index ON addresses (user_id, is_main) WHERE is_main = true;

-- Create unique constraint to ensure only one main address per user
CREATE UNIQUE INDEX addresses_user_id_main_uindex ON addresses (user_id) WHERE is_main = true;
