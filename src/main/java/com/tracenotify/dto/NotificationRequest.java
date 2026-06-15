package com.tracenotify.dto;

import com.tracenotify.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequest(
        @NotBlank String title,
        @NotBlank String message,
        @NotBlank String type,
        @NotNull Priority priority,
        @NotBlank String serviceName
) {}
