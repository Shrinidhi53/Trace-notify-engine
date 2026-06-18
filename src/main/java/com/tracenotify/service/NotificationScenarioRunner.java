package com.tracenotify.service;

import com.tracenotify.dto.NotificationEvent;
import com.tracenotify.exception.ApiException;
import com.tracenotify.kafka.NotificationProducer;
import com.tracenotify.model.Notification;
import com.tracenotify.model.NotificationStatus;
import com.tracenotify.model.Priority;
import com.tracenotify.repository.NotificationRepository;
import com.tracenotify.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runs individual notification scenarios in their own transactions (REQUIRES_NEW)
 * so that a failure in one scenario does not abort the others.
 *
 * All operations (save, publish, error-handling) stay in the SAME transaction.
 * The ApiException from contract validation is caught before it can mark the
 * transaction for rollback, so the catch block can safely update the notification.
 */
@Service
public class NotificationScenarioRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationScenarioRunner.class);

    private final NotificationRepository repository;
    private final NotificationProducer producer;
    private final CausalityService causalityService;
    private final UserRepository userRepository;

    public NotificationScenarioRunner(NotificationRepository repository,
                                      NotificationProducer producer,
                                      CausalityService causalityService,
                                      UserRepository userRepository) {
        this.repository = repository;
        this.producer = producer;
        this.causalityService = causalityService;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> runScenario(UUID userId, String title, String message,
                                            String serviceName, String eventType, Priority priority) {
        // Validate user exists BEFORE inserting (prevents FK violation)
        if (!userRepository.existsById(userId)) {
            throw new ApiException(400, "User not found. Please sign in again.");
        }

        // Save notification
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(eventType)
                .priority(priority)
                .status(NotificationStatus.UNREAD)
                .build();
        n = repository.save(n);
        causalityService.record(n.getId(), serviceName, eventType,
                Map.of("stage", "CREATED", "layer", "Database"));

        // Build Kafka event
        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(n.getId());
        event.setUserId(userId);
        event.setTitle(title);
        event.setMessage(message);
        event.setType(eventType);
        event.setPriority(priority);
        event.setServiceName(serviceName);

        Map<String, Object> result = new HashMap<>();
        result.put("id", n.getId());
        result.put("scenario", title);

        try {
            producer.publish(event);
            result.put("status", "SUCCESS");
        } catch (Exception e) {
            log.warn("Scenario '{}' failed: {}", title, e.getMessage());
            // Catch block runs in the SAME transaction — notification is visible.
            // ApiException is caught before Spring marks the tx for rollback.
            n.setStatus(NotificationStatus.FAILED);
            repository.save(n);

            Map<String, Object> errorMeta = new HashMap<>();
            errorMeta.put("stage", "ERROR");
            errorMeta.put("error", e.getMessage());
            errorMeta.put("errorType", e.getClass().getSimpleName());

            if (e.getMessage() != null) {
                if (e.getMessage().contains("not registered")) {
                    errorMeta.put("layer", "ContractValidation");
                    errorMeta.put("failurePoint", "Service registry check");
                } else if (e.getMessage().contains("not allowed")) {
                    errorMeta.put("layer", "ContractValidation");
                    errorMeta.put("failurePoint", "Event type validation");
                } else if (e.getMessage().contains("Rate limit")) {
                    errorMeta.put("layer", "ContractValidation");
                    errorMeta.put("failurePoint", "Rate limiter");
                } else {
                    errorMeta.put("layer", "KafkaProducer");
                    errorMeta.put("failurePoint", "Message publish");
                }
            }
            causalityService.record(n.getId(), serviceName, eventType, errorMeta);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
