/*
 * Profile
 */
CREATE TABLE profiles
(
    id                      BIGSERIAL
        CONSTRAINT profiles_id_pk
            PRIMARY KEY,
    name                    text                                                            NOT NULL,
    profile_picture_url     text,
    address                 text,
    created_at              timestamp WITH TIME ZONE        DEFAULT CURRENT_TIMESTAMP       NOT NULL,
    creator_id              bigint                                                          NOT NULL,
    updated_at              timestamp WITH TIME ZONE        DEFAULT CURRENT_TIMESTAMP       NOT NULL,
    updater_id              bigint                                                          NOT NULL,
    version                 bigint                          DEFAULT 0                       NOT NULL
);