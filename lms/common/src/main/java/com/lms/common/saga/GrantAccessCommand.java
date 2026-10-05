package com.lms.common.saga;

import java.util.UUID;

/** Enrollment -> Progress: "let this student open this course". */
public record GrantAccessCommand(UUID sagaId, Long enrollmentId, Long studentId, Long courseId) {}
