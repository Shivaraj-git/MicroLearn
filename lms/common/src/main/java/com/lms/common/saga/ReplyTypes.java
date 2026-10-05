package com.lms.common.saga;

public final class ReplyTypes {
    private ReplyTypes() {}
    public static final String PAYMENT_COMPLETED   = "PaymentCompleted";
    public static final String PAYMENT_FAILED      = "PaymentFailed";
    public static final String ACCESS_GRANTED      = "AccessGranted";
    public static final String ACCESS_GRANT_FAILED = "AccessGrantFailed";
    public static final String PAYMENT_REFUNDED    = "PaymentRefunded";
}
