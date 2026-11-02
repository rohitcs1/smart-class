CREATE TABLE tenants (
    id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenants_slug UNIQUE (slug)
);

CREATE TABLE institutions (
    id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80) NOT NULL,
    address VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_institutions_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_institutions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE campuses (
    id BINARY(16) NOT NULL,
    institution_id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_campuses_institution_name UNIQUE (institution_id, name),
    CONSTRAINT fk_campuses_institution FOREIGN KEY (institution_id) REFERENCES institutions(id)
);

CREATE TABLE buildings (
    id BINARY(16) NOT NULL,
    campus_id BINARY(16) NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_buildings_campus_name UNIQUE (campus_id, name),
    CONSTRAINT fk_buildings_campus FOREIGN KEY (campus_id) REFERENCES campuses(id)
);

CREATE TABLE floors (
    id BINARY(16) NOT NULL,
    building_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_floors_building_name UNIQUE (building_id, name),
    CONSTRAINT fk_floors_building FOREIGN KEY (building_id) REFERENCES buildings(id)
);

CREATE TABLE classrooms (
    id BINARY(16) NOT NULL,
    floor_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_classrooms_floor_name UNIQUE (floor_id, name),
    CONSTRAINT fk_classrooms_floor FOREIGN KEY (floor_id) REFERENCES floors(id)
);

CREATE TABLE subscription_plans (
    id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    billing_period VARCHAR(20) NOT NULL,
    price_inr DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_subscription_plans_name UNIQUE (name)
);

CREATE TABLE subscriptions (
    id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    plan_id BINARY(16),
    status VARCHAR(30) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    trial BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id)
);

CREATE TABLE discount_tokens (
    id BINARY(16) NOT NULL,
    code VARCHAR(100) NOT NULL,
    discount_percent DECIMAL(5,2) NOT NULL,
    applies_to VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_discount_tokens_code UNIQUE (code)
);

CREATE TABLE iot_devices (
    id BINARY(16) NOT NULL,
    classroom_id BINARY(16) NOT NULL,
    device_code VARCHAR(100) NOT NULL,
    setup_token_hash VARCHAR(255) NOT NULL,
    device_secret_hash VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    last_seen_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_iot_devices_code UNIQUE (device_code),
    CONSTRAINT fk_iot_devices_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
);

CREATE INDEX ix_institutions_tenant ON institutions(tenant_id);
CREATE INDEX ix_subscriptions_tenant_ends ON subscriptions(tenant_id, ends_at);
CREATE INDEX ix_devices_classroom_status ON iot_devices(classroom_id, status);
