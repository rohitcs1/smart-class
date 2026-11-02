ALTER TABLE users ADD COLUMN tenant_id BINARY(16);
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

CREATE TABLE payment_orders (
    id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    plan_id BINARY(16) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100),
    amount_paise BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    paid_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_orders_razorpay UNIQUE (razorpay_order_id),
    CONSTRAINT fk_payment_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_payment_orders_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);

CREATE INDEX ix_payment_orders_tenant ON payment_orders(tenant_id);
