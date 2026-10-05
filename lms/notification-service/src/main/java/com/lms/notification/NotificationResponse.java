package com.lms.notification;

import java.time.Instant;

public record NotificationResponse(Long id, String type, String message, Instant createdAt) {

    static NotificationResponse of(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getCreatedAt());
    }
}
