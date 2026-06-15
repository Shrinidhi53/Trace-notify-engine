package com.tracenotify.controller;

import com.tracenotify.model.ContractViolation;
import com.tracenotify.model.DlqMessage;
import com.tracenotify.repository.DlqMessageRepository;
import com.tracenotify.service.ContractService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ContractService contractService;
    private final DlqMessageRepository dlqMessageRepository;

    public AdminController(ContractService contractService, DlqMessageRepository dlqMessageRepository) {
        this.contractService = contractService;
        this.dlqMessageRepository = dlqMessageRepository;
    }

    @GetMapping("/violations")
    public List<ContractViolation> violations() {
        return contractService.listViolations();
    }

    @GetMapping("/dlq")
    public List<DlqMessage> dlq() {
        return dlqMessageRepository.findAllByOrderByReceivedAtDesc();
    }
}
