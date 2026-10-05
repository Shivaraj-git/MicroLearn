package com.lms.enrollment;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    protected Enrollment() {}

    public Enrollment(Long studentId, Long courseId, String courseTitle, BigDecimal price) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.price = price;
        this.status = EnrollmentStatus.PENDING;
    }

    public void confirm() { this.status = EnrollmentStatus.CONFIRMED; }
    public void reject()  { this.status = EnrollmentStatus.REJECTED; }
    public void cancel()  { this.status = EnrollmentStatus.CANCELLED; }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public BigDecimal getPrice() { return price; }
    public EnrollmentStatus getStatus() { return status; }
}
