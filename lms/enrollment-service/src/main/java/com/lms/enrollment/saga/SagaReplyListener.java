package com.lms.enrollment.saga;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.common.event.EnrollmentEvent;
import com.lms.common.saga.GrantAccessCommand;
import com.lms.common.saga.RefundPaymentCommand;
import com.lms.common.saga.ReplyTypes;
import com.lms.common.saga.SagaReply;
import com.lms.enrollment.Enrollment;
import com.lms.enrollment.EnrollmentRepository;
import com.lms.enrollment.config.RabbitConfig;
import com.lms.enrollment.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The orchestrator. Reads replies and decides the next step:
 *
 *   PAYMENT_PENDING --PaymentCompleted--> ACCESS_PENDING --AccessGranted--> COMPLETED (CONFIRMED)
 *   PAYMENT_PENDING --PaymentFailed-----> FAILED (REJECTED)
 *   ACCESS_PENDING  --AccessGrantFailed-> COMPENSATING --PaymentRefunded--> FAILED (CANCELLED)
 */
@Component
public class SagaReplyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaReplyListener.class);

    private final EnrollmentSagaRepository sagas;
    private final EnrollmentRepository enrollments;
    private final OutboxWriter outbox;

    public SagaReplyListener(EnrollmentSagaRepository sagas, EnrollmentRepository enrollments,
                             OutboxWriter outbox) {
        this.sagas = sagas;
        this.enrollments = enrollments;
        this.outbox = outbox;
    }

    @RabbitListener(queues = RabbitConfig.REPLY_QUEUE)
    @Transactional
    public void on(SagaReply reply) {
        EnrollmentSaga saga = sagas.findById(reply.sagaId().toString()).orElse(null);
        if (saga == null) {
            log.warn("Reply for unknown saga {}", reply.sagaId());
            return;
        }
        Enrollment enrollment = enrollments.findById(saga.getEnrollmentId()).orElseThrow();

        switch (reply.type()) {

            case ReplyTypes.PAYMENT_COMPLETED -> {
                if (!expect(saga, SagaState.PAYMENT_PENDING, reply)) return;
                saga.awaitAccess();
                outbox.add(Exchanges.COMMANDS, RoutingKeys.PROGRESS_GRANT_ACCESS,
                        new GrantAccessCommand(reply.sagaId(), enrollment.getId(),
                                enrollment.getStudentId(), enrollment.getCourseId()));
                log.info("Enrollment {}: paid, asking Progress to grant access", enrollment.getId());
            }

            case ReplyTypes.PAYMENT_FAILED -> {
                if (!expect(saga, SagaState.PAYMENT_PENDING, reply)) return;
                enrollment.reject();
                saga.fail();
                log.info("Enrollment {} REJECTED: {}", enrollment.getId(), reply.reason());
                publish(enrollment, RoutingKeys.ENROLLMENT_REJECTED);
            }

            case ReplyTypes.ACCESS_GRANTED -> {
                if (!expect(saga, SagaState.ACCESS_PENDING, reply)) return;
                enrollment.confirm();
                saga.complete();
                log.info("Enrollment {} CONFIRMED", enrollment.getId());
                publish(enrollment, RoutingKeys.ENROLLMENT_CONFIRMED);
            }

            case ReplyTypes.ACCESS_GRANT_FAILED -> {
                if (!expect(saga, SagaState.ACCESS_PENDING, reply)) return;
                saga.compensate();
                outbox.add(Exchanges.COMMANDS, RoutingKeys.PAYMENT_REFUND,
                        new RefundPaymentCommand(reply.sagaId(), enrollment.getId()));
                log.warn("Enrollment {}: access failed ({}), asking Payment for a refund",
                        enrollment.getId(), reply.reason());
            }

            case ReplyTypes.PAYMENT_REFUNDED -> {
                if (!expect(saga, SagaState.COMPENSATING, reply)) return;
                enrollment.cancel();
                saga.fail();
                log.info("Enrollment {} CANCELLED (payment refunded)", enrollment.getId());
                publish(enrollment, RoutingKeys.ENROLLMENT_CANCELLED);
            }

            default -> log.warn("Unknown reply type {}", reply.type());
        }
    }

    /** Announces the final result to anyone who cares (e.g. the Notification service). */
    private void publish(Enrollment e, String routingKey) {
        outbox.add(Exchanges.EVENTS, routingKey,
                new EnrollmentEvent(e.getId(), e.getStudentId(), e.getCourseId(),
                        e.getCourseTitle(), e.getStatus().name()));
    }

    /** Duplicate or out-of-order messages are ignored, so the saga can't jump around. */
    private boolean expect(EnrollmentSaga saga, SagaState expected, SagaReply reply) {
        if (saga.getState() == expected) return true;
        log.info("Ignoring {} for saga {}: state is {}, expected {}",
                reply.type(), reply.sagaId(), saga.getState(), expected);
        return false;
    }
}
