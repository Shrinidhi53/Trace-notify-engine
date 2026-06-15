package com.tracenotify.controller;

import com.tracenotify.dto.CausalityResponse;
import com.tracenotify.service.CausalityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CausalityController {

    private final CausalityService causalityService;

    public CausalityController(CausalityService causalityService) {
        this.causalityService = causalityService;
    }

    @GetMapping("/notifications/{id}/trace")
    public CausalityResponse trace(@PathVariable UUID id) {
        return causalityService.getTrace(id);
    }
}
