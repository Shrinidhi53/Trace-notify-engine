package com.tracenotify.dto;

import com.tracenotify.model.Priority;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ContractRequest(
        @NotNull String serviceName,
        @NotEmpty List<String> allowedEventTypes,
        @Positive int maxFrequencyPerMinute,
        @NotNull Priority priorityLevel
) {}
