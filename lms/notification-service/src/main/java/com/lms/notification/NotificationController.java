package com.lms.notification;

import com.lms.notification.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) { this.repo = repo; }

    @GetMapping("/my")
    public List<NotificationResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return repo.findByStudentIdOrderByIdDesc(user.id())
                .stream().map(NotificationResponse::of).toList();
    }
}
