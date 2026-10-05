package com.lms.common.messaging;

public final class MessageHeaders {
    private MessageHeaders() {}
    public static final String MESSAGE_ID     = "x-message-id";     // idempotency key
    public static final String CORRELATION_ID = "x-correlation-id"; // saga id / trace
}
