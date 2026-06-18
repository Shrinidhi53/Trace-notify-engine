package com.tracenotify.service;

import com.tracenotify.dto.NotificationEvent;
import com.tracenotify.dto.NotificationRequest;
import com.tracenotify.dto.NotificationResponse;
import com.tracenotify.exception.ApiException;
import com.tracenotify.kafka.NotificationProducer;
import com.tracenotify.model.Notification;
import com.tracenotify.model.NotificationStatus;
import com.tracenotify.model.Priority;
import com.tracenotify.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationProducer producer;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPreferenceService preferenceService;
    private final CausalityService causalityService;
    private final NotificationScenarioRunner scenarioRunner;

    public NotificationService(NotificationRepository repository,
                               NotificationProducer producer,
                               SimpMessagingTemplate messagingTemplate,
                               UserPreferenceService preferenceService,
                               CausalityService causalityService,
                               NotificationScenarioRunner scenarioRunner) {
        this.repository = repository;
        this.producer = producer;
        this.messagingTemplate = messagingTemplate;
        this.preferenceService = preferenceService;
        this.causalityService = causalityService;
        this.scenarioRunner = scenarioRunner;
    }

    /**
     * Sends a notification via the scenario runner so that if contract validation
     * fails, the error is recorded in a separate transaction (PostgreSQL-safe).
     * No @Transactional here - the scenario runner manages its own transaction.
     */
    public NotificationResponse send(UUID userId, NotificationRequest req) {
        Map<String, Object> result = scenarioRunner.runScenario(
                userId, req.title(), req.message(),
                req.serviceName(), req.type(), req.priority());

        // Re-fetch the notification to return the latest status
        UUID notifId = (UUID) result.get("id");
        Notification n = repository.findById(notifId).orElseThrow();
        return NotificationResponse.from(n);
    }

    /** Called by the Kafka consumer after an event is consumed. */
    public void deliver(NotificationEvent event) {
        causalityService.record(event.getNotificationId(), "KafkaConsumer",
                "EVENT_CONSUMED", Map.of("stage", "CONSUMED", "layer", "KafkaConsumer"));

        if (!preferenceService.isInAppEnabled(event.getUserId())) {
            causalityService.record(event.getNotificationId(), "NotificationService",
                    "WS_SKIPPED", Map.of("stage", "SKIPPED", "layer", "PreferenceCheck",
                    "reason", "In-app notifications disabled for user"));
            log.info("In-app disabled for user {}, skipping WS push", event.getUserId());
            return;
        }

        try {
            NotificationResponse payload = new NotificationResponse(
                    event.getNotificationId(), event.getUserId(), event.getTitle(),
                    event.getMessage(), event.getType(), event.getPriority(),
                    NotificationStatus.UNREAD, null);

            messagingTemplate.convertAndSendToUser(
                    event.getUserId().toString(), "/queue/notifications", payload);

            causalityService.record(event.getNotificationId(), "WebSocketService",
                    "WS_PUSHED", Map.of("stage", "PUSHED", "layer", "WebSocket"));
            log.info("Pushed notification {} to user {}", event.getNotificationId(), event.getUserId());
        } catch (Exception e) {
            causalityService.record(event.getNotificationId(), "WebSocketService",
                    "WS_FAILED", Map.of("stage", "ERROR", "layer", "WebSocket",
                    "error", e.getMessage(), "errorType", e.getClass().getSimpleName()));
            throw e;
        }
    }

    /**
     * Runs failure simulation scenarios. Each scenario runs in its own REQUIRES_NEW
     * transaction, so a failure in one does not abort the others.
     */
    public List<Map<String, Object>> simulateFailures(UUID userId) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Scenario 1: Unregistered service (fails at ContractValidation - service registry)
        results.add(scenarioRunner.runScenario(userId,
                "Scenario: Unregistered Service",
                "This notification uses a service name not registered in any contract.",
                "unknown-service", "ALERT", Priority.HIGH));

        // Scenario 2: Disallowed event type (fails at ContractValidation - event type check)
        results.add(scenarioRunner.runScenario(userId,
                "Scenario: Wrong Event Type",
                "This notification uses an event type not allowed by the auth-service contract.",
                "auth-service", "INVALID_TYPE", Priority.MEDIUM));

        // Scenario 3: Successful delivery (passes all layers)
        results.add(scenarioRunner.runScenario(userId,
                "Scenario: Successful Delivery",
                "This notification passes all checks and is delivered successfully.",
                "auth-service", "LOGIN", Priority.MEDIUM));

        return results;
    }

    public Page<NotificationResponse> history(UUID userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID id) {
        Notification n = ownedOrThrow(userId, id);
        n.setStatus(NotificationStatus.READ);
        return NotificationResponse.from(repository.save(n));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Notification n = ownedOrThrow(userId, id);
        repository.delete(n);
    }

    private Notification ownedOrThrow(UUID userId, UUID id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new ApiException(404, "Notification not found"));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(403, "Not your notification");
        }
        return n;
    }
}
