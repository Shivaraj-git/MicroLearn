package com.lms.enrollment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Call this INSIDE a transaction: the message is saved together with your data. */
@Component
public class OutboxWriter {

    private final OutboxRepository repo;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public void add(String exchange, String routingKey, Object payload) {
        try {
            repo.save(new OutboxEvent(exchange, routingKey, mapper.writeValueAsString(payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
