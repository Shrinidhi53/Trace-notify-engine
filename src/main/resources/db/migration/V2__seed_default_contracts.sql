-- Default service contracts so notifications work out of the box
INSERT INTO service_contracts (service_name, allowed_event_types, max_frequency_per_minute, priority_level, created_at)
VALUES
    ('auth-service',    '["LOGIN","LOGOUT","SIGNUP","PASSWORD_RESET"]'::jsonb, 120, 'MEDIUM', now()),
    ('payment-service', '["PAYMENT_SUCCESS","PAYMENT_FAILED","REFUND"]'::jsonb, 60, 'HIGH', now()),
    ('order-service',   '["ORDER_CREATED","ORDER_SHIPPED","ORDER_DELIVERED","ORDER_CANCELLED"]'::jsonb, 100, 'MEDIUM', now()),
    ('system',          '["ALERT","MAINTENANCE","UPDATE","NOTIFICATION"]'::jsonb, 200, 'LOW', now()),
    ('notification-service', '["NOTIFICATION_SENT","NOTIFICATION_READ","NOTIFICATION_DELETED"]'::jsonb, 300, 'LOW', now());
