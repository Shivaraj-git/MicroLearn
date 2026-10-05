package com.lms.notification.config;

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

    public static final String QUEUE = "notification.enrollment-events";
    public static final String DLQ   = "notification.enrollment-events.dlq";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(Exchanges.DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue notificationDlq() { return QueueBuilder.durable(DLQ).build(); }

    // One queue, listening to three kinds of events
    @Bean
    Binding confirmedBinding(@Qualifier("notificationQueue") Queue q,
                             @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.ENROLLMENT_CONFIRMED);
    }

    @Bean
    Binding rejectedBinding(@Qualifier("notificationQueue") Queue q,
                            @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.ENROLLMENT_REJECTED);
    }

    @Bean
    Binding cancelledBinding(@Qualifier("notificationQueue") Queue q,
                             @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.ENROLLMENT_CANCELLED);
    }

    @Bean
    Binding dlqBinding(@Qualifier("notificationDlq") Queue q,
                       @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
