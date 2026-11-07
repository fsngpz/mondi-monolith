/*
 * Orders created by the storefront checkout.
 * The legacy `transactions` table stays as is for admin-created receipts.
 */
CREATE TYPE order_status as enum (
    'PENDING_PAYMENT', 'PAYMENT_CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'
);
CREATE TYPE payment_method as enum ('BANK_TRANSFER', 'WHATSAPP');

/*
 * Orders
 */
CREATE TABLE orders
(
    id                      BIGSERIAL
        CONSTRAINT orders_id_pk
            PRIMARY KEY,
    profile_id              bigint
        CONSTRAINT orders_profile_id_fk
            REFERENCES profiles
            ON UPDATE CASCADE ON DELETE SET NULL,
    status                  order_status             DEFAULT 'PENDING_PAYMENT' NOT NULL,
    payment_method          payment_method                                     NOT NULL,
    shipping_recipient_name text                                               NOT NULL,
    shipping_phone          text                                               NOT NULL,
    shipping_address_line1  text                                               NOT NULL,
    shipping_address_line2  text,
    shipping_city           text                                               NOT NULL,
    shipping_state          text                                               NOT NULL,
    shipping_postal_code    text                                               NOT NULL,
    shipping_country        text                                               NOT NULL,
    subtotal                numeric(19, 4)                                     NOT NULL,
    discount_amount         numeric(19, 4)           DEFAULT 0                 NOT NULL,
    total_price             numeric(19, 4)                                     NOT NULL,
    voucher_code            text,
    payment_proof_url       text,
    notes                   text,
    created_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id              text                                               NOT NULL,
    updated_at              timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id              text                                               NOT NULL,
    version                 bigint                   DEFAULT 0                 NOT NULL,
    CONSTRAINT check_orders_amounts_valid
        CHECK (subtotal >= 0 AND discount_amount >= 0 AND total_price >= 0)
);

CREATE INDEX orders_profile_id_index ON orders (profile_id);
CREATE INDEX orders_status_index ON orders (status);
CREATE INDEX orders_created_at_index ON orders (created_at DESC);

/*
 * Order Items
 */
CREATE TABLE order_items
(
    id              BIGSERIAL
        CONSTRAINT order_items_id_pk
            PRIMARY KEY,
    order_id        bigint
        CONSTRAINT order_items_order_id_fk
            REFERENCES orders
            ON UPDATE CASCADE ON DELETE CASCADE                        NOT NULL,
    product_id      bigint
        CONSTRAINT order_items_product_id_fk
            REFERENCES products
            ON UPDATE CASCADE ON DELETE SET NULL,
    product_name    text                                               NOT NULL,
    product_sku     text                                               NOT NULL,
    price           numeric(19, 4)                                     NOT NULL,
    quantity        integer                                            NOT NULL,
    certificate_url text,
    created_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id      text                                               NOT NULL,
    updated_at      timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id      text                                               NOT NULL,
    version         bigint                   DEFAULT 0                 NOT NULL,
    CONSTRAINT check_order_items_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX order_items_order_id_index ON order_items (order_id);
CREATE INDEX order_items_product_id_index ON order_items (product_id);
