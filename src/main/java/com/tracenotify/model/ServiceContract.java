package com.tracenotify.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceContract {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "service_name", nullable = false, unique = true)
    private String serviceName;

    @Type(JsonType.class)
    @Column(name = "allowed_event_types", columnDefinition = "jsonb", nullable = false)
    private List<String> allowedEventTypes;

    @Column(name = "max_frequency_per_minute", nullable = false)
    private int maxFrequencyPerMinute;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private Priority priorityLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (priorityLevel == null) priorityLevel = Priority.MEDIUM;
    }
}
