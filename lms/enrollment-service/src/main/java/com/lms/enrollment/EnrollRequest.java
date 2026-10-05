package com.lms.enrollment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** paymentMethod is fake for now: use TEST_SUCCESS or TEST_FAIL. */
public record EnrollRequest(@NotNull Long courseId, @NotBlank String paymentMethod) {}
