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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationProducer producer;
    private SimpMessagingTemplate messagingTemplate;
    private UserPreferenceService preferenceService;
    private CausalityService causalityService;
    private NotificationScenarioRunner scenarioRunner;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        producer = mock(NotificationProducer.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        preferenceService = mock(UserPreferenceService.class);
        causalityService = mock(CausalityService.class);
        scenarioRunner = mock(NotificationScenarioRunner.class);
        service = new NotificationService(repository, producer, messagingTemplate,
                preferenceService, causalityService, scenarioRunner);
    }

    @Test
    void sendDelegatesToScenarioRunner() {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        Notification n = Notification.builder().id(notifId).userId(userId)
                .title("Title").message("Body").type("ORDER_PLACED")
                .priority(Priority.HIGH).status(NotificationStatus.UNREAD).build();

        Map<String, Object> result = new HashMap<>();
        result.put("id", notifId);
        result.put("status", "SUCCESS");
        when(scenarioRunner.runScenario(eq(userId), eq("Title"), eq("Body"),
                eq("OrderService"), eq("ORDER_PLACED"), eq(Priority.HIGH))).thenReturn(result);
        when(repository.findById(notifId)).thenReturn(Optional.of(n));

        NotificationRequest req = new NotificationRequest(
                "Title", "Body", "ORDER_PLACED", Priority.HIGH, "OrderService");
        NotificationResponse resp = service.send(userId, req);

        assertNotNull(resp);
        assertEquals("Title", resp.title());
        verify(scenarioRunner).runScenario(eq(userId), eq("Title"), eq("Body"),
                eq("OrderService"), eq("ORDER_PLACED"), eq(Priority.HIGH));
    }

    @Test
    void deliverPushesWhenEnabled() {
        UUID userId = UUID.randomUUID();
        when(preferenceService.isInAppEnabled(userId)).thenReturn(true);
        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(UUID.randomUUID());
        event.setUserId(userId);
        event.setTitle("T");
        event.setMessage("M");
        event.setType("ORDER_PLACED");
        event.setPriority(Priority.LOW);

        service.deliver(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void deliverSkipsWhenDisabled() {
        UUID userId = UUID.randomUUID();
        when(preferenceService.isInAppEnabled(userId)).thenReturn(false);
        NotificationEvent event = new NotificationEvent();
        event.setNotificationId(UUID.randomUUID());
        event.setUserId(userId);
        event.setType("ORDER_PLACED");

        service.deliver(event);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void markAsReadRejectsForeignNotification() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Notification n = Notification.builder().id(id).userId(owner)
                .status(NotificationStatus.UNREAD).build();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(n));

        assertThrows(ApiException.class, () -> service.markAsRead(attacker, id));
    }
}
