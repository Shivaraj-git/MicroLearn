package com.lms.common.saga;

import java.util.UUID;

/** Reply sent by a participant back to the Enrollment orchestrator. */
public record SagaReply(UUID sagaId, String type, String reason) {}
