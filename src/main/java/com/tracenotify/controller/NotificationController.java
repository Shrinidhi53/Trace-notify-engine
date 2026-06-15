package com.tracenotify.controller;

import com.tracenotify.dto.NotificationRequest;
import com.tracenotify.dto.NotificationResponse;
import com.tracenotify.security.CurrentUser;
import com.tracenotify.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/send")
    public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
        return service.send(CurrentUser.id(), request);
    }

    @GetMapping("/me")
    public Page<NotificationResponse> myHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.history(CurrentUser.id(), page, size);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return service.markAsRead(CurrentUser.id(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }
}
