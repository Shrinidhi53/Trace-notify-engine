package com.tracenotify.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqMessage {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) receivedAt = Instant.now();
    }
}
