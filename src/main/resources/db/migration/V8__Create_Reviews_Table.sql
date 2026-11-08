/*
 * Product reviews. A customer can review a product once, after an order containing it is delivered.
 */
CREATE TABLE reviews
(
    id          BIGSERIAL
        CONSTRAINT reviews_id_pk
            PRIMARY KEY,
    profile_id  bigint
        CONSTRAINT reviews_profile_id_fk
            REFERENCES profiles
            ON UPDATE CASCADE ON DELETE CASCADE                    NOT NULL,
    product_id  bigint
        CONSTRAINT reviews_product_id_fk
            REFERENCES products
            ON UPDATE CASCADE ON DELETE SET NULL,
    order_id    bigint
        CONSTRAINT reviews_order_id_fk
            REFERENCES orders
            ON UPDATE CASCADE ON DELETE SET NULL,
    rating      integer                                            NOT NULL,
    comment     text,
    created_at  timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id  text                                               NOT NULL,
    updated_at  timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id  text                                               NOT NULL,
    version     bigint                   DEFAULT 0                 NOT NULL,
    CONSTRAINT check_reviews_rating_range CHECK (rating >= 1 AND rating <= 5)
);

CREATE UNIQUE INDEX reviews_profile_product_uindex ON reviews (profile_id, product_id);
CREATE INDEX reviews_product_id_index ON reviews (product_id);
CREATE INDEX reviews_profile_id_index ON reviews (profile_id);
