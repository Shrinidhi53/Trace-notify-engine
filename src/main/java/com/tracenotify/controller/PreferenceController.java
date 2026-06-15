package com.tracenotify.controller;

import com.tracenotify.dto.PreferenceRequest;
import com.tracenotify.dto.PreferenceResponse;
import com.tracenotify.security.CurrentUser;
import com.tracenotify.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {

    private final UserPreferenceService service;

    public PreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public PreferenceResponse get() {
        return service.get(CurrentUser.id());
    }

    @PutMapping
    public PreferenceResponse update(@Valid @RequestBody PreferenceRequest request) {
        return service.update(CurrentUser.id(), request.inAppEnabled());
    }
}
