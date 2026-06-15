package com.tracenotify.service;

import com.tracenotify.dto.NotificationEvent;
import com.tracenotify.dto.NotificationRequest;
import com.tracenotify.dto.NotificationResponse;
import com.tracenotify.exception.ApiException;
import com.tracenotify.kafka.NotificationProducer;
import com.tracenotify.model.Notification;
import com.tracenotify.model.NotificationStatus;
import com.tracenotify.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public NotificationService(NotificationRepository repository,
                               NotificationProducer producer,
                               SimpMessagingTemplate messagingTemplate,
                               UserPreferenceService preferenceService,
                               CausalityService causalityService) {
        this.repository = repository;
        this.producer = producer;
        this.messagingTemplate = messagingTemplate;
        this.preferenceService = preferenceService;
        this.causalityService = causalityService;
    }

    @Transactional
    public NotificationResponse send(UUID userId, NotificationRequest req) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(req.title())
                .message(req.message())
                .type(req.type())
                .priority(req.priority())
                .status(NotificationStatus.UNREAD)
                .build();
        notification = repository.save(notification);

        causalityService.record(notification.getId(), req.serviceName(), req.type(),
                Map.of("stage", "CREATED"));

        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(notification.getId());
        event.setUserId(userId);
        event.setTitle(req.title());
        event.setMessage(req.message());
        event.setType(req.type());
        event.setPriority(req.priority());
        event.setServiceName(req.serviceName());

        // Contract check happens inside the producer before publishing to Kafka.
        producer.publish(event);

        return NotificationResponse.from(notification);
    }

    /** Called by the Kafka consumer after an event is consumed. */
    public void deliver(NotificationEvent event) {
        causalityService.record(event.getNotificationId(), "NotificationService",
                "EVENT_CONSUMED", Map.of("stage", "CONSUMED"));

        if (!preferenceService.isInAppEnabled(event.getUserId())) {
            log.info("In-app disabled for user {}, skipping WS push", event.getUserId());
            return;
        }

        NotificationResponse payload = new NotificationResponse(
                event.getNotificationId(), event.getUserId(), event.getTitle(),
                event.getMessage(), event.getType(), event.getPriority(),
                NotificationStatus.UNREAD, null);

        messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(), "/queue/notifications", payload);

        causalityService.record(event.getNotificationId(), "NotificationService",
                "WS_PUSHED", Map.of("stage", "PUSHED"));
        log.info("Pushed notification {} to user {}", event.getNotificationId(), event.getUserId());
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
