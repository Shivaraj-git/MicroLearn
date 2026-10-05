package com.lms.payment;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.common.saga.RefundPaymentCommand;
import com.lms.common.saga.ReplyTypes;
import com.lms.common.saga.SagaReply;
import com.lms.payment.config.RabbitConfig;
import com.lms.payment.outbox.OutboxWriter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** The COMPENSATION step: undo a payment when a later saga step failed. */
@Component
public class PaymentRefundHandler {

    private final PaymentRepository payments;
    private final OutboxWriter outbox;

    public PaymentRefundHandler(PaymentRepository payments, OutboxWriter outbox) {
        this.payments = payments;
        this.outbox = outbox;
    }

    @RabbitListener(queues = RabbitConfig.REFUND_QUEUE)
    @Transactional
    public void on(RefundPaymentCommand cmd) {
        Payment payment = payments.findBySagaId(cmd.sagaId().toString())
                .orElseThrow(() -> new IllegalStateException("No payment for saga " + cmd.sagaId()));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.refund();   // (a real gateway call would go here)
        }
        // if already REFUNDED we just reply again, so duplicates are harmless

        outbox.add(Exchanges.EVENTS, RoutingKeys.ENROLLMENT_SAGA_REPLY,
                new SagaReply(cmd.sagaId(), ReplyTypes.PAYMENT_REFUNDED, null));
    }
}
