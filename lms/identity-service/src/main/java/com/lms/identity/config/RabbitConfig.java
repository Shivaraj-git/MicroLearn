package com.lms.identity.config;

import com.lms.common.messaging.Exchanges;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange eventsExchange() { return new TopicExchange(Exchanges.EVENTS, true, false); }
}
