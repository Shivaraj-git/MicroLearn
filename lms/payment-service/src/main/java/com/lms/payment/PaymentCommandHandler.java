package com.lms.payment;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.common.saga.ProcessPaymentCommand;
import com.lms.common.saga.ReplyTypes;
import com.lms.common.saga.SagaReply;
import com.lms.payment.config.RabbitConfig;
import com.lms.payment.outbox.OutboxWriter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receives "charge this student" and replies with the result.
 * This is a FAKE gateway: paymentMethod "TEST_FAIL" is declined, everything else succeeds.
 */
@Component
public class PaymentCommandHandler {

    private final PaymentRepository payments;
    private final OutboxWriter outbox;

    public PaymentCommandHandler(PaymentRepository payments, OutboxWriter outbox) {
        this.payments = payments;
        this.outbox = outbox;
    }

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    @Transactional
    public void on(ProcessPaymentCommand cmd) {
        String sagaId = cmd.sagaId().toString();

        // If this message arrives twice, we reuse the first result instead of charging twice.
        Payment payment = payments.findBySagaId(sagaId).orElseGet(() -> {
            boolean ok = !"TEST_FAIL".equalsIgnoreCase(cmd.paymentMethod());
            return payments.save(new Payment(sagaId, cmd.enrollmentId(), cmd.studentId(),
                    cmd.amount(), cmd.paymentMethod(),
                    ok ? PaymentStatus.COMPLETED : PaymentStatus.FAILED));
        });

        boolean completed = payment.getStatus() != PaymentStatus.FAILED;
        SagaReply reply = new SagaReply(cmd.sagaId(),
                completed ? ReplyTypes.PAYMENT_COMPLETED : ReplyTypes.PAYMENT_FAILED,
                completed ? null : "Payment declined");

        outbox.add(Exchanges.EVENTS, RoutingKeys.ENROLLMENT_SAGA_REPLY, reply);
    }
}
