package com.lms.course.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateCourseRequest(
        @NotBlank @Size(max = 200, message = "Title must be 200 characters or fewer") String title,
        @Size(max = 1000, message = "Description must be 1,000 characters or fewer") String description,
        @NotNull @DecimalMin("0.0") @DecimalMax(value = "99999999.99", message = "Price is too large") BigDecimal price) {}
