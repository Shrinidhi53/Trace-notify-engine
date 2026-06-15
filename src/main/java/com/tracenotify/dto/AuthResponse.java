package com.tracenotify.dto;

public record AuthResponse(String token, String userId, String username, String role) {}
