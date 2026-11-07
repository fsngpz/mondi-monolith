/*
 * Loyalty points ledger and discount vouchers.
 */
CREATE TYPE loyalty_transaction_type as enum ('EARNED', 'REDEEMED', 'EXPIRED', 'ADJUSTMENT');
CREATE TYPE voucher_type as enum ('PERCENTAGE', 'FIXED');

/*
 * Loyalty points: one row per change; the balance is the sum.
 */
CREATE TABLE loyalty_points
(
    id               BIGSERIAL
        CONSTRAINT loyalty_points_id_pk
            PRIMARY KEY,
    profile_id       bigint
        CONSTRAINT loyalty_points_profile_id_fk
            REFERENCES profiles
            ON UPDATE CASCADE ON DELETE CASCADE                         NOT NULL,
    points           integer                                            NOT NULL,
    transaction_type loyalty_transaction_type                           NOT NULL,
    order_id         bigint
        CONSTRAINT loyalty_points_order_id_fk
            REFERENCES orders
            ON UPDATE CASCADE ON DELETE SET NULL,
    description      text,
    created_at       timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id       text                                               NOT NULL,
    updated_at       timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id       text                                               NOT NULL,
    version          bigint                   DEFAULT 0                 NOT NULL
);

CREATE INDEX loyalty_points_profile_id_index ON loyalty_points (profile_id);
-- an order earns points once
CREATE UNIQUE INDEX loyalty_points_order_earned_uindex ON loyalty_points (order_id) WHERE transaction_type = 'EARNED';

/*
 * Vouchers
 */
CREATE TABLE vouchers
(
    id                  BIGSERIAL
        CONSTRAINT vouchers_id_pk
            PRIMARY KEY,
    code                text                                               NOT NULL,
    description         text,
    type                voucher_type                                       NOT NULL,
    value               numeric(19, 4)                                     NOT NULL,
    max_discount_amount numeric(19, 4),
    min_order_amount    numeric(19, 4)           DEFAULT 0                 NOT NULL,
    max_usage           integer,
    current_usage       integer                  DEFAULT 0                 NOT NULL,
    start_date          timestamp WITH TIME ZONE                           NOT NULL,
    end_date            timestamp WITH TIME ZONE                           NOT NULL,
    is_active           boolean                  DEFAULT true              NOT NULL,
    created_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    creator_id          text                                               NOT NULL,
    updated_at          timestamp WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updater_id          text                                               NOT NULL,
    version             bigint                   DEFAULT 0                 NOT NULL,
    CONSTRAINT check_vouchers_value_positive CHECK (value > 0),
    CONSTRAINT check_vouchers_percentage_max CHECK (type <> 'PERCENTAGE' OR value <= 100),
    CONSTRAINT check_vouchers_usage CHECK (current_usage >= 0 AND (max_usage IS NULL OR current_usage <= max_usage)),
    CONSTRAINT check_vouchers_dates CHECK (start_date < end_date)
);

CREATE UNIQUE INDEX vouchers_code_uindex ON vouchers (UPPER(code));
CREATE INDEX vouchers_is_active_index ON vouchers (is_active);
