package com.tracenotify.service;

import com.tracenotify.model.Priority;
import com.tracenotify.model.ServiceContract;
import com.tracenotify.repository.ContractViolationRepository;
import com.tracenotify.repository.ServiceContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractValidationServiceTest {

    private ServiceContractRepository contractRepository;
    private ContractViolationRepository violationRepository;
    private ContractValidationService service;

    @BeforeEach
    void setUp() {
        contractRepository = mock(ServiceContractRepository.class);
        violationRepository = mock(ContractViolationRepository.class);
        service = new ContractValidationService(contractRepository, violationRepository);
    }

    private ServiceContract contract(int maxPerMin) {
        return ServiceContract.builder()
                .serviceName("OrderService")
                .allowedEventTypes(List.of("ORDER_PLACED"))
                .maxFrequencyPerMinute(maxPerMin)
                .priorityLevel(Priority.HIGH)
                .build();
    }

    @Test
    void allowsValidEvent() {
        when(contractRepository.findByServiceName("OrderService"))
                .thenReturn(Optional.of(contract(10)));
        var result = service.validate("OrderService", "ORDER_PLACED");
        assertTrue(result.allowed);
        verify(violationRepository, never()).save(any());
    }

    @Test
    void rejectsUnregisteredService() {
        when(contractRepository.findByServiceName("Ghost")).thenReturn(Optional.empty());
        var result = service.validate("Ghost", "ANY");
        assertFalse(result.allowed);
        verify(violationRepository).save(any());
    }

    @Test
    void rejectsDisallowedEventType() {
        when(contractRepository.findByServiceName("OrderService"))
                .thenReturn(Optional.of(contract(10)));
        var result = service.validate("OrderService", "PAYMENT_FAILED");
        assertFalse(result.allowed);
        assertTrue(result.reason.contains("not allowed"));
    }

    @Test
    void enforcesRateLimit() {
        when(contractRepository.findByServiceName("OrderService"))
                .thenReturn(Optional.of(contract(2)));
        assertTrue(service.validate("OrderService", "ORDER_PLACED").allowed);
        assertTrue(service.validate("OrderService", "ORDER_PLACED").allowed);
        var third = service.validate("OrderService", "ORDER_PLACED");
        assertFalse(third.allowed);
        assertTrue(third.reason.contains("Rate limit"));

        ArgumentCaptor<com.tracenotify.model.ContractViolation> captor =
                ArgumentCaptor.forClass(com.tracenotify.model.ContractViolation.class);
        verify(violationRepository, atLeastOnce()).save(captor.capture());
    }
}
