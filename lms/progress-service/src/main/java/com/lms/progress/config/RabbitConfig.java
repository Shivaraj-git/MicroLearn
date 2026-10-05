package com.lms.progress.config;

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

    public static final String COMMAND_QUEUE = "progress.commands";
    public static final String COMMAND_DLQ   = "progress.commands.dlq";

    @Bean TopicExchange eventsExchange()   { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean TopicExchange commandsExchange() { return new TopicExchange(Exchanges.COMMANDS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    @Bean
    Queue commandQueue() {
        return QueueBuilder.durable(COMMAND_QUEUE)
                .deadLetterExchange(Exchanges.DLX)
                .deadLetterRoutingKey(COMMAND_DLQ)
                .build();
    }

    @Bean
    Queue commandDlq() { return QueueBuilder.durable(COMMAND_DLQ).build(); }

    @Bean
    Binding commandBinding(@Qualifier("commandQueue") Queue q,
                           @Qualifier("commandsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.PROGRESS_GRANT_ACCESS);
    }

    @Bean
    Binding commandDlqBinding(@Qualifier("commandDlq") Queue q,
                              @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(COMMAND_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}
