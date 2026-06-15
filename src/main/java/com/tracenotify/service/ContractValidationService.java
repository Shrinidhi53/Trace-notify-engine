package com.tracenotify.service;

import com.tracenotify.model.ContractViolation;
import com.tracenotify.model.ServiceContract;
import com.tracenotify.repository.ContractViolationRepository;
import com.tracenotify.repository.ServiceContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContractValidationService {

    private static final Logger log = LoggerFactory.getLogger(ContractValidationService.class);

    private final ServiceContractRepository contractRepository;
    private final ContractViolationRepository violationRepository;

    // Sliding-window rate limiter: serviceName -> timestamps (epoch ms) within last minute
    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    public ContractValidationService(ServiceContractRepository contractRepository,
                                     ContractViolationRepository violationRepository) {
        this.contractRepository = contractRepository;
        this.violationRepository = violationRepository;
    }

    public static class ValidationResult {
        public final boolean allowed;
        public final String reason;
        private ValidationResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }
        static ValidationResult ok() { return new ValidationResult(true, null); }
        static ValidationResult deny(String reason) { return new ValidationResult(false, reason); }
    }

    public ValidationResult validate(String serviceName, String eventType) {
        Optional<ServiceContract> contractOpt = contractRepository.findByServiceName(serviceName);
        if (contractOpt.isEmpty()) {
            return reject(serviceName, eventType, "Service not registered");
        }
        ServiceContract contract = contractOpt.get();

        if (contract.getAllowedEventTypes() == null
                || !contract.getAllowedEventTypes().contains(eventType)) {
            return reject(serviceName, eventType, "Event type not allowed by contract");
        }

        if (!withinRateLimit(serviceName, contract.getMaxFrequencyPerMinute())) {
            return reject(serviceName, eventType, "Rate limit exceeded ("
                    + contract.getMaxFrequencyPerMinute() + "/min)");
        }
        return ValidationResult.ok();
    }

    private boolean withinRateLimit(String serviceName, int maxPerMinute) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 60_000L;
        Deque<Long> timestamps = windows.computeIfAbsent(serviceName, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxPerMinute) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private ValidationResult reject(String serviceName, String eventType, String reason) {
        log.warn("Contract violation: service={} event={} reason={}", serviceName, eventType, reason);
        violationRepository.save(ContractViolation.builder()
                .serviceName(serviceName)
                .eventTypeAttempted(eventType)
                .reason(reason)
                .build());
        return ValidationResult.deny(reason);
    }
}
