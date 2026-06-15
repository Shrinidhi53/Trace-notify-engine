package com.tracenotify.controller;

import com.tracenotify.dto.ContractRequest;
import com.tracenotify.model.ContractViolation;
import com.tracenotify.model.ServiceContract;
import com.tracenotify.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@PreAuthorize("hasRole('ADMIN')")
public class ContractController {

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ServiceContract register(@Valid @RequestBody ContractRequest request) {
        return service.register(request);
    }

    @GetMapping
    public List<ServiceContract> list() {
        return service.listAll();
    }

    @GetMapping("/violations")
    public List<ContractViolation> violations() {
        return service.listViolations();
    }
}
