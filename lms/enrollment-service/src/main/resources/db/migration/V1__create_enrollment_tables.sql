CREATE TABLE enrollments (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT        NOT NULL,
  course_id  BIGINT        NOT NULL,
  price      DECIMAL(10,2) NOT NULL,
  status     VARCHAR(20)   NOT NULL,
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The saga's memory: where is each enrollment in the process?
CREATE TABLE enrollment_sagas (
  saga_id       VARCHAR(36) PRIMARY KEY,
  enrollment_id BIGINT      NOT NULL,
  state         VARCHAR(30) NOT NULL,
  version       BIGINT      NOT NULL,
  updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE outbox_event (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  exchange_name VARCHAR(100)  NOT NULL,
  routing_key   VARCHAR(100)  NOT NULL,
  payload       VARCHAR(4000) NOT NULL,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at       TIMESTAMP     NULL
);
