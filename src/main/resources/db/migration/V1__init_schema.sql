CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id);

CREATE TABLE notification_causality (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    step_order INT NOT NULL,
    service_name VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT now(),
    metadata JSONB
);
CREATE INDEX idx_causality_notification ON notification_causality(notification_id);

CREATE TABLE service_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(150) NOT NULL UNIQUE,
    allowed_event_types JSONB NOT NULL,
    max_frequency_per_minute INT NOT NULL DEFAULT 60,
    priority_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE contract_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(150) NOT NULL,
    event_type_attempted VARCHAR(100),
    reason VARCHAR(500) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE dlq_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payload TEXT NOT NULL,
    error_reason VARCHAR(1000),
    received_at TIMESTAMP NOT NULL DEFAULT now()
);
