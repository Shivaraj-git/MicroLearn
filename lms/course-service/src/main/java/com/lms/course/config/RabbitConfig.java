package com.lms.course.config;

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

    public static final String PROJECTOR_QUEUE = "course.query.projector";
    public static final String PROJECTOR_DLQ   = "course.query.projector.dlq";
    public static final String RENAME_QUEUE    = "course.user-renamed";
    public static final String RENAME_DLQ      = "course.user-renamed.dlq";

    @Bean TopicExchange eventsExchange() { return new TopicExchange(Exchanges.EVENTS, true, false); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(Exchanges.DLX, true, false); }

    @Bean Queue projectorQueue() { return withDlq(PROJECTOR_QUEUE, PROJECTOR_DLQ); }
    @Bean Queue projectorDlq()   { return QueueBuilder.durable(PROJECTOR_DLQ).build(); }
    @Bean Queue renameQueue()    { return withDlq(RENAME_QUEUE, RENAME_DLQ); }
    @Bean Queue renameDlq()      { return QueueBuilder.durable(RENAME_DLQ).build(); }

    @Bean
    Binding projectorBinding(@Qualifier("projectorQueue") Queue q, @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with("course.changed");
    }

    @Bean
    Binding renameBinding(@Qualifier("renameQueue") Queue q, @Qualifier("eventsExchange") TopicExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RoutingKeys.USER_RENAMED);
    }

    @Bean
    Binding projectorDlqBinding(@Qualifier("projectorDlq") Queue q, @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(PROJECTOR_DLQ);
    }

    @Bean
    Binding renameDlqBinding(@Qualifier("renameDlq") Queue q, @Qualifier("deadLetterExchange") DirectExchange ex) {
        return BindingBuilder.bind(q).to(ex).with(RENAME_DLQ);
    }

    @Bean
    MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    private Queue withDlq(String name, String dlq) {
        return QueueBuilder.durable(name).deadLetterExchange(Exchanges.DLX).deadLetterRoutingKey(dlq).build();
    }
}
