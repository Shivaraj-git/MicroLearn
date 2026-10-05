package com.lms.course.command;

import java.math.BigDecimal;

public record AdminCourseView(Long id, String title, BigDecimal price, String status,
                              String instructorEmail, int lessonCount) {

    static AdminCourseView of(Course c) {
        return new AdminCourseView(c.getId(), c.getTitle(), c.getPrice(), c.getStatus().name(),
                c.getInstructorEmail(), c.getLessons().size());
    }
}
