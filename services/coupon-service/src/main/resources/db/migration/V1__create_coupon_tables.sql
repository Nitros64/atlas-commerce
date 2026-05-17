CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    discount_type VARCHAR(30) NOT NULL,
    discount_value NUMERIC(19,2) NOT NULL,
    minimum_order_amount NUMERIC(19,2) NOT NULL,
    max_uses INTEGER NOT NULL,
    current_uses INTEGER NOT NULL,
    active BOOLEAN NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_coupon_code ON coupons(code);