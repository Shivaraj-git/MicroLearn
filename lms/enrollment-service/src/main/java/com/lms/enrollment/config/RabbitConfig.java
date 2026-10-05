package com.lms.enrollment.config;

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

    public static final String REPLY_QUEUE = "enrollment.saga.replies";
    public static final String REPLY_DLQ   = "enrollment.saga.replies.dlq";

    @Bean TopicExchange eventsExchange()   { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean TopicExchange commandsExchange() { return new TopicExchange(Exchanges.COMMANDS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    @Bean
    Queue replyQueue() {
        return QueueBuilder.durable(REPLY_QUEUE)
                .deadLetterExchange(Exchanges.DLX)
                .deadLetterRoutingKey(REPLY_DLQ)
                .build();
    }

    @Bean
    Queue replyDlq() { return QueueBuilder.durable(REPLY_DLQ).build(); }

    @Bean
    Binding replyBinding(@Qualifier("replyQueue") Queue q,
                         @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.ENROLLMENT_SAGA_REPLY);
    }

    @Bean
    Binding replyDlqBinding(@Qualifier("replyDlq") Queue q,
                            @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(REPLY_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
