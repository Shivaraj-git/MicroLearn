package com.lms.enrollment.saga;

public enum SagaState {
    PAYMENT_PENDING,   // waiting for Payment
    ACCESS_PENDING,    // paid, waiting for Progress to unlock the course
    COMPENSATING,      // Progress failed, waiting for Payment to refund
    COMPLETED,         // success
    FAILED             // ended without success (rejected or cancelled)
}
