package com.lms.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    private String type;
    private String message;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(Long studentId, Long enrollmentId, String type, String message) {
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.type = type;
        this.message = message;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
