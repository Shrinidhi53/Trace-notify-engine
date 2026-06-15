package com.tracenotify.dto;

import jakarta.validation.constraints.NotNull;

public record PreferenceRequest(@NotNull Boolean inAppEnabled) {}
