CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    sku VARCHAR(255) NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    minimum_stock_level INTEGER NOT NULL,
    warehouse_location VARCHAR(255),
    last_updated TIMESTAMP WITH TIME ZONE
);