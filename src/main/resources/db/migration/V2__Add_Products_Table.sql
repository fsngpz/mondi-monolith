CREATE TYPE product_category as enum ('RING', 'EARRING', 'NECKLACE', 'BRACELET', 'PENDANT', 'OTHER');
CREATE TYPE product_status as enum ('ACTIVE', 'INACTIVE');

/*
 * Products
 */
CREATE TABLE products
(
    id                      BIGSERIAL
        CONSTRAINT products_id_pk
            PRIMARY KEY,
    name                    text                                               NOT NULL,
    description             text,
    price                   text                                               NOT NULL,
    currency                text                                               NOT NULL,
    specification_in_html   text,
    discount_percentage     numeric(5, 2)            DEFAULT 0.00              NOT NULL,
    category                product_category         DEFAULT 'OTHER'           NOT NULL,
    stock                   integer                  DEFAULT 0                 NOT NULL,
    sku                     text                                               NOT NULL UNIQUE,
    status                  product_status           DEFAULT 'ACTIVE'          NOT NULL,
    created_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id              text                                               NOT NULL,
    updated_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id              text                                               NOT NULL,
    version                 bigint                   DEFAULT 0                 NOT NULL
);

CREATE INDEX products_category_index ON products (category);
CREATE INDEX products_price_index ON products (price);
CREATE INDEX products_sku_index ON products (sku);
CREATE INDEX products_status_index ON products (status);

/*
 * Product Media
 */
CREATE TABLE product_media
(
    id                      BIGSERIAL
        CONSTRAINT product_media_id_pk
            PRIMARY KEY,
    product_id              bigint
        CONSTRAINT product_media_product_id_fk
            REFERENCES products
            ON UPDATE CASCADE ON DELETE CASCADE,
    media_url               text                                               NOT NULL,
    display_order           integer                  DEFAULT 0                 NOT NULL,
    created_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id              text                                               NOT NULL,
    updated_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id              text                                               NOT NULL,
    version                 bigint                   DEFAULT 0                 NOT NULL
);

CREATE INDEX product_media_product_id_index ON product_media (product_id);
CREATE INDEX product_media_display_order_index ON product_media (display_order);
