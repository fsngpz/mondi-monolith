/*
 * Users
 */
CREATE TABLE users
(
    id         BIGSERIAL
        CONSTRAINT users_id_pk
            PRIMARY KEY,
    email               text                                               NOT NULL,
    password            text                                               NOT NULL,
    username            text,
    roles               text                                               NOT NULL,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);

CREATE UNIQUE INDEX users_email_uindex ON users (LOWER(email));


/*
 * Profile
 */
CREATE TABLE profiles
(
    id                  BIGSERIAL
        CONSTRAINT profiles_id_pk
            PRIMARY KEY,
    name                text                                               NOT NULL,
    profile_picture_url text,
    address             text,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);