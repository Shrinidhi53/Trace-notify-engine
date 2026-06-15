package com.tracenotify.dto;

import java.util.List;
import java.util.UUID;

public record CausalityResponse(UUID notificationId, List<Step> causality) {
    public record Step(int step, String service, String event, String timestamp) {}
}
