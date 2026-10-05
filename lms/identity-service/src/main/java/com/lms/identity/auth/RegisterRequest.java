package com.lms.identity.auth;

import com.lms.identity.user.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 8, message = "password must be at least 8 characters") String password,
        @NotNull Role role) {}
