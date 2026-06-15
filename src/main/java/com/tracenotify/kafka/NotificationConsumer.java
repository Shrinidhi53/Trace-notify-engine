package com.tracenotify.kafka;

import com.tracenotify.dto.NotificationEvent;
import com.tracenotify.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.inapp}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(NotificationEvent event, Acknowledgment ack) {
        log.info("Consumed notification event {}", event.getNotificationId());
        notificationService.deliver(event);
        ack.acknowledge();
    }
}
