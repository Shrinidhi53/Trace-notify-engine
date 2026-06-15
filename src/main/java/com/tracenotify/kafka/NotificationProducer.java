package com.tracenotify.kafka;

import com.tracenotify.dto.NotificationEvent;
import com.tracenotify.exception.ApiException;
import com.tracenotify.service.CausalityService;
import com.tracenotify.service.ContractValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final ContractValidationService contractValidationService;
    private final CausalityService causalityService;

    @Value("${app.kafka.topics.inapp}")
    private String inappTopic;

    public NotificationProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                                ContractValidationService contractValidationService,
                                CausalityService causalityService) {
        this.kafkaTemplate = kafkaTemplate;
        this.contractValidationService = contractValidationService;
        this.causalityService = causalityService;
    }

    public void publish(NotificationEvent event) {
        // Contract check BEFORE any event hits Kafka
        ContractValidationService.ValidationResult result =
                contractValidationService.validate(event.getServiceName(), event.getType());
        if (!result.allowed) {
            throw new ApiException(422, "Contract violation: " + result.reason);
        }
        causalityService.record(event.getNotificationId(), event.getServiceName(),
                event.getType(), Map.of("stage", "PRODUCED"));
        kafkaTemplate.send(inappTopic, event.getUserId().toString(), event);
        log.info("Published notification event {} to {}", event.getNotificationId(), inappTopic);
    }
}
