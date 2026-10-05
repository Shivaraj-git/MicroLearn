package com.lms.common.event;

/** Published by Progress when a student finishes every lesson of a course. */
public record CourseCompletedEvent(Long studentId, String studentEmail, Long courseId, String courseTitle) {}
