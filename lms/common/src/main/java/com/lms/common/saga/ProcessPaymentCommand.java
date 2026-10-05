package com.lms.common.saga;

import java.math.BigDecimal;
import java.util.UUID;

/** Enrollment -> Payment: "please charge this student". */
public record ProcessPaymentCommand(UUID sagaId, Long enrollmentId, Long studentId,
                                    BigDecimal amount, String paymentMethod) {}
