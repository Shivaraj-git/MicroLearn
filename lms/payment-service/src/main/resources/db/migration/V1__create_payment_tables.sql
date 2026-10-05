CREATE TABLE payments (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  saga_id        VARCHAR(36)   NOT NULL UNIQUE,   -- one payment per saga = safe against duplicates
  enrollment_id  BIGINT        NOT NULL,
  student_id     BIGINT        NOT NULL,
  amount         DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(50)   NOT NULL,
  status         VARCHAR(20)   NOT NULL,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_event (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  exchange_name VARCHAR(100)  NOT NULL,
  routing_key   VARCHAR(100)  NOT NULL,
  payload       VARCHAR(4000) NOT NULL,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at       TIMESTAMP     NULL
);
