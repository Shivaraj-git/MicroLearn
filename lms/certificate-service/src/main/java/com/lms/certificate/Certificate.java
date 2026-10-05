package com.lms.certificate;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "certificates")
public class Certificate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_code")
    private String code;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_email")
    private String studentEmail;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "issued_at", insertable = false, updatable = false)
    private Instant issuedAt;

    protected Certificate() {}

    public Certificate(String code, Long studentId, String studentEmail, Long courseId, String courseTitle) {
        this.code = code;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
    }

    public String getCode() { return code; }
    public Long getCourseId() { return courseId; }
    public String getStudentEmail() { return studentEmail; }
    public String getCourseTitle() { return courseTitle; }
    public Instant getIssuedAt() { return issuedAt; }
}
