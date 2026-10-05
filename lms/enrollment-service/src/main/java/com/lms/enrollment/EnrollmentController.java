package com.lms.enrollment;

import com.lms.enrollment.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) { this.service = service; }

    /** 202 = "accepted, still being processed". Check the status afterwards. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EnrollmentResponse enroll(@Valid @RequestBody EnrollRequest req,
                                     @AuthenticationPrincipal AuthUser user) {
        return service.enroll(req, user);
    }

    @GetMapping("/my")
    public List<EnrollmentResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return service.mine(user);
    }

    @GetMapping("/{id}")
    public EnrollmentResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.get(id, user);
    }
}
