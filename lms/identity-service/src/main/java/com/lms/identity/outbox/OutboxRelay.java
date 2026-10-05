package com.lms.identity.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Every second: send unsent outbox rows to RabbitMQ. If RabbitMQ is down, they wait. */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repo;
    private final RabbitTemplate rabbit;

    public OutboxRelay(OutboxRepository repo, RabbitTemplate rabbit) {
        this.repo = repo;
        this.rabbit = rabbit;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pending = repo.findTop50BySentAtIsNullOrderByIdAsc();
        for (OutboxEvent e : pending) {
            try {
                Message msg = MessageBuilder
                        .withBody(e.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .setMessageId(String.valueOf(e.getId()))
                        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                        .build();
                rabbit.invoke(t -> {
                    t.send(e.getExchangeName(), e.getRoutingKey(), msg);
                    t.waitForConfirmsOrDie(5000);
                    return null;
                });
                e.markSent();
            } catch (Exception ex) {
                log.warn("Could not send to RabbitMQ, will retry: {}", ex.getMessage());
                return;
            }
        }
    }
}
