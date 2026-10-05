package com.lms.notification;

import com.lms.common.event.EnrollmentEvent;
import com.lms.notification.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reacts to enrollment results. Enrollment does not know this service exists. */
@Component
public class EnrollmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentEventListener.class);

    private final NotificationRepository repo;

    public EnrollmentEventListener(NotificationRepository repo) { this.repo = repo; }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void on(EnrollmentEvent e) {
        if (repo.existsByEnrollmentIdAndType(e.enrollmentId(), e.status())) {
            return;   // duplicate message, already notified
        }

        String title = (e.courseTitle() == null || e.courseTitle().isBlank())
                ? "course #" + e.courseId() : "\"" + e.courseTitle() + "\"";

        String message = switch (e.status()) {
            case "CONFIRMED" -> "Welcome! You are now enrolled in " + title + ".";
            case "REJECTED"  -> "Your payment for " + title + " was declined. Please try again.";
            case "CANCELLED" -> "We could not unlock " + title + ", so your payment was refunded.";
            default          -> null;
        };
        if (message == null) return;

        repo.save(new Notification(e.studentId(), e.enrollmentId(), e.status(), message));

        // A real system would call an email/SMS provider here.
        log.info("EMAIL (simulated) to student {}: {}", e.studentId(), message);
    }
}
