package com.lms.course.query;

import java.math.BigDecimal;

public record CatalogItem(Long courseId, String title, String description,
                          BigDecimal price, String instructorEmail, int lessonCount,
                          String instructorName) {}
