package com.lms.identity.auth;

public record AuthResponse(String token, Long userId, String name, String role) {}
