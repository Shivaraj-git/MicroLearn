package com.lms.enrollment;

public record EnrollmentResponse(Long enrollmentId, Long courseId, String status, String courseTitle) {

    static EnrollmentResponse of(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getCourseId(), e.getStatus().name(), e.getCourseTitle());
    }
}
