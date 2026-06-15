package com.tracenotify.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contract_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractViolation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "event_type_attempted")
    private String eventTypeAttempted;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    void onCreate() {
        if (timestamp == null) timestamp = Instant.now();
    }
}
