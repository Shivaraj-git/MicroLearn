package com.lms.identity.outbox;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_name")
    private String exchangeName;

    @Column(name = "routing_key")
    private String routingKey;

    private String payload;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEvent() {}

    public OutboxEvent(String exchangeName, String routingKey, String payload) {
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.payload = payload;
    }

    public void markSent() { this.sentAt = Instant.now(); }

    public Long getId() { return id; }
    public String getExchangeName() { return exchangeName; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
}
