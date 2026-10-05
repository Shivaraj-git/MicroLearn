package com.lms.identity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfileRequests {
    private ProfileRequests() {}

    public record UpdateProfile(
            @NotBlank @Size(max = 100, message = "Name must be 100 characters or fewer") String name) {}

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "New password must be 8 to 72 characters") String newPassword) {}
}
