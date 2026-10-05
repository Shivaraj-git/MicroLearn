package com.lms.course.command;

import java.math.BigDecimal;

public record CourseSummary(Long id, String title, String description,
                            BigDecimal price, String status, int lessonCount) {

    static CourseSummary of(Course c) {
        return new CourseSummary(c.getId(), c.getTitle(), c.getDescription(), c.getPrice(),
                c.getStatus().name(), c.getLessons().size());
    }
}
