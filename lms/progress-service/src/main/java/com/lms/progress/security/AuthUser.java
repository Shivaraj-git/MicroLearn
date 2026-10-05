package com.lms.progress.security;

/** The logged-in person, read from the JWT. */
public record AuthUser(Long id, String email, String role) {}
