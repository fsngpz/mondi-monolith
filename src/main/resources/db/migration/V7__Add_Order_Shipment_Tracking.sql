/*
 * Shipment tracking for orders: who delivers it, the courier's tracking number (resi)
 * and when it was shipped. Set from the backoffice once the payment is confirmed.
 */
CREATE TYPE shipping_courier as enum (
    'JNE', 'JNT', 'SICEPAT', 'ANTERAJA', 'POS_INDONESIA', 'TIKI', 'NINJA_XPRESS', 'IN_HOUSE'
);

ALTER TABLE orders
    ADD COLUMN shipping_courier shipping_courier,
    ADD COLUMN tracking_number  text,
    ADD COLUMN shipped_at       timestamp WITH TIME ZONE;
