package com.tracenotify.kafka;

import com.tracenotify.model.DlqMessage;
import com.tracenotify.repository.DlqMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repository;

    public DlqConsumer(DlqMessageRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.topics.dlq}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(Object payload, Acknowledgment ack) {
        log.warn("Storing DLQ message for inspection");
        repository.save(DlqMessage.builder()
                .payload(String.valueOf(payload))
                .errorReason("Delivered to DLQ after retries exhausted")
                .build());
        ack.acknowledge();
    }
}
