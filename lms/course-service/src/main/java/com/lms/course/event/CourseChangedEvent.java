package com.lms.course.event;

import java.math.BigDecimal;

/**
 * "A published course changed." The query side refreshes the catalog from it.
 * removed = true means "take this course OUT of the catalog".
 */
public record CourseChangedEvent(Long courseId, String title, String description,
                                 BigDecimal price, String instructorEmail, String instructorName,
                                 int lessonCount, boolean removed) {}
