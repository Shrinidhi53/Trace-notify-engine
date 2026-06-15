package com.tracenotify.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification_causality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCausality {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant timestamp;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }
}
