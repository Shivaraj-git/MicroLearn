package com.lms.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudentIdOrderByIdDesc(Long studentId);
    boolean existsByEnrollmentIdAndType(Long enrollmentId, String type);
}
