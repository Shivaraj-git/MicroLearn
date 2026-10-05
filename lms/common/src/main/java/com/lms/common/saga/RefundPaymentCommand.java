package com.lms.common.saga;

import java.util.UUID;

/** Enrollment -> Payment: "give the money back" (compensation). */
public record RefundPaymentCommand(UUID sagaId, Long enrollmentId) {}
