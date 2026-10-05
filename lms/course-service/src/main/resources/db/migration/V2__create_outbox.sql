-- Messages waiting to be sent to RabbitMQ.
-- Saved in the SAME transaction as the course change, so nothing gets lost.
CREATE TABLE outbox_event (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  routing_key VARCHAR(100)  NOT NULL,
  payload     VARCHAR(4000) NOT NULL,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at     TIMESTAMP     NULL
);
