package com.lms.course.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLessonRequest(
        @NotBlank @Size(max = 200, message = "Title must be 200 characters or fewer") String title,
        @Size(max = 50000, message = "Lesson content must be 50,000 characters or fewer") String content) {}
