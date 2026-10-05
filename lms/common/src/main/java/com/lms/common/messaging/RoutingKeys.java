package com.lms.common.messaging;

public final class RoutingKeys {
    private RoutingKeys() {}
    // commands
    public static final String PAYMENT_PROCESS        = "payment.process";        // -> Payment
    public static final String PAYMENT_REFUND         = "payment.refund";         // -> Payment (compensation)
    public static final String PROGRESS_GRANT_ACCESS  = "progress.grant-access";  // -> Progress
    // saga replies
    public static final String ENROLLMENT_SAGA_REPLY  = "enrollment.saga.reply";  // replies -> Enrollment
    // domain events (anyone can listen)
    public static final String ENROLLMENT_CONFIRMED   = "enrollment.confirmed";
    public static final String ENROLLMENT_REJECTED    = "enrollment.rejected";
    public static final String ENROLLMENT_CANCELLED   = "enrollment.cancelled";
    public static final String COURSE_COMPLETED       = "course.completed";
    public static final String USER_RENAMED           = "user.renamed";
}
