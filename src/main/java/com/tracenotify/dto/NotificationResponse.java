package com.tracenotify.dto;

import com.tracenotify.model.Notification;
import com.tracenotify.model.NotificationStatus;
import com.tracenotify.model.Priority;

import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String title,
        String message,
        String type,
        Priority priority,
        NotificationStatus status,
        String createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getUserId(), n.getTitle(), n.getMessage(),
                n.getType(), n.getPriority(), n.getStatus(),
                n.getCreatedAt() == null ? null : n.getCreatedAt().toString());
    }
}
