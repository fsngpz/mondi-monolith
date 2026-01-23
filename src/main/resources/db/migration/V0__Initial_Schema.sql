CREATE TYPE oauth_provider as enum ('LOCAL', 'GOOGLE');

/*
 * Users
 */
CREATE TABLE users
(
    id         BIGSERIAL
        CONSTRAINT users_id_pk
            PRIMARY KEY,
    email               text                                               NOT NULL,
    password            text,
    username            text,
    provider            oauth_provider            DEFAULT 'LOCAL'          NOT NULL,
    provider_id         text,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);

CREATE UNIQUE INDEX users_email_uindex ON users (LOWER(email));
CREATE UNIQUE INDEX users_provider_provider_id_uindex ON users (provider, provider_id) WHERE provider_id IS NOT NULL;

-- Add constraint to ensure password is not null when provider is LOCAL
ALTER TABLE users ADD CONSTRAINT check_password_for_local_provider
    CHECK (
        (provider = 'LOCAL' AND password IS NOT NULL) OR
        (provider != 'LOCAL' AND provider_id IS NOT NULL)
        );

/*
 * Roles
 */
CREATE TABLE roles
(
    id          BIGSERIAL
        CONSTRAINT roles_id_pk
            PRIMARY KEY,
    name                text                                               NOT NULL,
    description         text,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);

CREATE UNIQUE INDEX roles_name_uindex ON roles(UPPER(name));


/*
 * Users Roles
 */
CREATE TABLE users_roles
(
    id                  BIGSERIAL
        CONSTRAINT users_roles_id_pk
            PRIMARY KEY,
    user_id             bigint
        CONSTRAINT users_roles_user_id_fk
            REFERENCES users
            ON UPDATE CASCADE ON DELETE CASCADE,
    role_id             bigint
        CONSTRAINT users_roles_role_id_fk
            REFERENCES roles
            ON UPDATE CASCADE ON DELETE CASCADE,
    version             bigint                   DEFAULT 0                 NOT NULL
);

/*
 * Profile
 */
CREATE TABLE profiles
(
    id                  bigint
        CONSTRAINT profiles_id_pk
            PRIMARY KEY
        CONSTRAINT profiles_users_id_fk
            REFERENCES users
            ON UPDATE CASCADE ON DELETE CASCADE,
    name                text,
    profile_picture_url text,
    address             text,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);

/*
 * Transaction
 */
CREATE TABLE transactions
(
    id              BIGSERIAL
        CONSTRAINT transactions_id_pk
            PRIMARY KEY,
    profile_id      bigint
        CONSTRAINT transactions_profile_id_fk
            REFERENCES profiles
            ON UPDATE CASCADE ON DELETE CASCADE,
    product_name    text                                               NOT NULL,
    price           text                                               NOT NULL,
    certificate_url text                                               NOT NULL,
    purchased_at    timestamp WITH TIME ZONE                           NOT NULL,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text                                               NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text                                               NOT NULL,
    version         bigint                   DEFAULT 0                 NOT NULL
);
