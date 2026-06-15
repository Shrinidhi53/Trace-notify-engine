package com.tracenotify.service;

import com.tracenotify.dto.ContractRequest;
import com.tracenotify.exception.ApiException;
import com.tracenotify.model.ContractViolation;
import com.tracenotify.model.ServiceContract;
import com.tracenotify.repository.ContractViolationRepository;
import com.tracenotify.repository.ServiceContractRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractService {

    private final ServiceContractRepository contractRepository;
    private final ContractViolationRepository violationRepository;

    public ContractService(ServiceContractRepository contractRepository,
                           ContractViolationRepository violationRepository) {
        this.contractRepository = contractRepository;
        this.violationRepository = violationRepository;
    }

    public ServiceContract register(ContractRequest req) {
        if (contractRepository.existsByServiceName(req.serviceName())) {
            throw new ApiException(409, "Contract already exists for service");
        }
        return contractRepository.save(ServiceContract.builder()
                .serviceName(req.serviceName())
                .allowedEventTypes(req.allowedEventTypes())
                .maxFrequencyPerMinute(req.maxFrequencyPerMinute())
                .priorityLevel(req.priorityLevel())
                .build());
    }

    public List<ServiceContract> listAll() {
        return contractRepository.findAll();
    }

    public List<ContractViolation> listViolations() {
        return violationRepository.findAllByOrderByTimestampDesc();
    }
}
