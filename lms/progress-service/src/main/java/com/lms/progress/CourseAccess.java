package com.lms.progress;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "course_access")
public class CourseAccess {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected CourseAccess() {}

    public CourseAccess(Long studentId, Long courseId, Long enrollmentId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrollmentId = enrollmentId;
    }

    public Long getCourseId() { return courseId; }

    public boolean isCompleted() { return completedAt != null; }
    public void markCompleted() { this.completedAt = Instant.now(); }
}
