package com.lms.common.event;

/** Published by Enrollment when an enrollment reaches its final state. status: CONFIRMED / REJECTED / CANCELLED */
public record EnrollmentEvent(Long enrollmentId, Long studentId, Long courseId,
                              String courseTitle, String status) {}
