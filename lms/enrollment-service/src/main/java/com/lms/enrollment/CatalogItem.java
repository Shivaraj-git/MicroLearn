package com.lms.enrollment;

import java.math.BigDecimal;

/** What the Course service's catalog returns. */
public record CatalogItem(Long courseId, String title, String description,
                          BigDecimal price, String instructorEmail, int lessonCount) {}
