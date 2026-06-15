package com.tracenotify.service;

import com.tracenotify.dto.CausalityResponse;
import com.tracenotify.model.NotificationCausality;
import com.tracenotify.repository.NotificationCausalityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CausalityService {

    private final NotificationCausalityRepository repository;

    public CausalityService(NotificationCausalityRepository repository) {
        this.repository = repository;
    }

    public void record(UUID notificationId, String serviceName, String eventType, Map<String, Object> metadata) {
        int nextStep = repository.findByNotificationIdOrderByStepOrderAsc(notificationId).size() + 1;
        repository.save(NotificationCausality.builder()
                .notificationId(notificationId)
                .stepOrder(nextStep)
                .serviceName(serviceName)
                .eventType(eventType)
                .metadata(metadata)
                .build());
    }

    public CausalityResponse getTrace(UUID notificationId) {
        List<CausalityResponse.Step> steps = repository
                .findByNotificationIdOrderByStepOrderAsc(notificationId)
                .stream()
                .map(c -> new CausalityResponse.Step(
                        c.getStepOrder(),
                        c.getServiceName(),
                        c.getEventType(),
                        c.getTimestamp().toString()))
                .toList();
        return new CausalityResponse(notificationId, steps);
    }
}
