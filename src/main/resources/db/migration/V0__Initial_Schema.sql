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
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL
);

CREATE UNIQUE INDEX users_email_uindex ON users (LOWER(email));

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