package com.lms.payment.config;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PAYMENT_QUEUE = "payment.commands";
    public static final String PAYMENT_DLQ   = "payment.commands.dlq";
    public static final String REFUND_QUEUE  = "payment.refunds";
    public static final String REFUND_DLQ    = "payment.refunds.dlq";

    @Bean TopicExchange eventsExchange()   { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean TopicExchange commandsExchange() { return new TopicExchange(Exchanges.COMMANDS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    // ---- charge ----
    @Bean Queue paymentQueue() { return dlqEnabled(PAYMENT_QUEUE, PAYMENT_DLQ); }
    @Bean Queue paymentDlq()   { return QueueBuilder.durable(PAYMENT_DLQ).build(); }

    // ---- refund ----
    @Bean Queue refundQueue()  { return dlqEnabled(REFUND_QUEUE, REFUND_DLQ); }
    @Bean Queue refundDlq()    { return QueueBuilder.durable(REFUND_DLQ).build(); }

    @Bean
    Binding paymentBinding(@Qualifier("paymentQueue") Queue q, @Qualifier("commandsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.PAYMENT_PROCESS);
    }

    @Bean
    Binding refundBinding(@Qualifier("refundQueue") Queue q, @Qualifier("commandsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.PAYMENT_REFUND);
    }

    @Bean
    Binding paymentDlqBinding(@Qualifier("paymentDlq") Queue q, @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(PAYMENT_DLQ);
    }

    @Bean
    Binding refundDlqBinding(@Qualifier("refundDlq") Queue q, @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(REFUND_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    private Queue dlqEnabled(String name, String dlq) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(Exchanges.DLX)
                .deadLetterRoutingKey(dlq)
                .build();
    }
}
