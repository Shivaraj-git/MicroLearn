package com.lms.certificate.config;

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

    public static final String QUEUE = "certificate.course-completed";
    public static final String DLQ   = "certificate.course-completed.dlq";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    @Bean
    Queue certificateQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(Exchanges.DLX)
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue certificateDlq() { return QueueBuilder.durable(DLQ).build(); }

    @Bean
    Binding certificateBinding(@Qualifier("certificateQueue") Queue q,
                               @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.COURSE_COMPLETED);
    }

    @Bean
    Binding dlqBinding(@Qualifier("certificateDlq") Queue q,
                       @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
