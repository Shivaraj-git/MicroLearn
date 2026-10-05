package com.lms.course.command;

import java.math.BigDecimal;
import java.util.List;

public record CourseDetail(Long id, String title, String description, BigDecimal price,
                           String status, List<LessonView> lessons) {

    static CourseDetail of(Course c) {
        return new CourseDetail(c.getId(), c.getTitle(), c.getDescription(), c.getPrice(),
                c.getStatus().name(), c.getLessons().stream().map(LessonView::of).toList());
    }
}
