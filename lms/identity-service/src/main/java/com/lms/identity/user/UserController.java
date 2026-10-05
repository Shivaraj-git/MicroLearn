package com.lms.identity.user;

import com.lms.identity.auth.AuthResponse;
import com.lms.identity.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;
    private final ProfileService profile;
    private final JwtService jwt;

    public UserController(UserRepository users, ProfileService profile, JwtService jwt) {
        this.users = users;
        this.profile = profile;
        this.jwt = jwt;
    }

    /** Needs a valid token. Returns the logged-in user's details. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User u = users.findById(Long.valueOf(auth.getName())).orElseThrow();
        return Map.of("id", u.getId(), "name", u.getName(), "email", u.getEmail(), "role", u.getRole());
    }

    /** Change your name. Returns a fresh token, because the name is stored inside the token. */
    @PutMapping("/me")
    public AuthResponse updateProfile(@Valid @RequestBody ProfileRequests.UpdateProfile req, Authentication auth) {
        User u = profile.rename(Long.valueOf(auth.getName()), req.name().trim());
        return new AuthResponse(jwt.generate(u), u.getId(), u.getName(), u.getRole().name());
    }

    @PutMapping("/me/password")
    public Map<String, String> changePassword(@Valid @RequestBody ProfileRequests.ChangePassword req,
                                              Authentication auth) {
        profile.changePassword(Long.valueOf(auth.getName()), req.currentPassword(), req.newPassword());
        return Map.of("message", "Password changed");
    }
}
