package com.tracenotify.dto;

import com.tracenotify.model.Priority;

import java.io.Serializable;
import java.util.UUID;

public class NotificationEvent implements Serializable {
    private UUID notificationId;
    private UUID userId;
    private String title;
    private String message;
    private String type;
    private Priority priority;
    private String serviceName;

    public NotificationEvent() {}

    public UUID getNotificationId() { return notificationId; }
    public void setNotificationId(UUID notificationId) { this.notificationId = notificationId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
}
