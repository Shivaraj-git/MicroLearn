CREATE TABLE outbox_event (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  exchange_name VARCHAR(100)  NOT NULL,
  routing_key   VARCHAR(100)  NOT NULL,
  payload       VARCHAR(4000) NOT NULL,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at       TIMESTAMP     NULL
);
