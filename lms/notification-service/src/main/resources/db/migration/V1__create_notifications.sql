CREATE TABLE notifications (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id    BIGINT       NOT NULL,
  enrollment_id BIGINT       NOT NULL,
  type          VARCHAR(30)  NOT NULL,
  message       VARCHAR(500) NOT NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- same event twice = same notification once
  UNIQUE KEY uq_enrollment_type (enrollment_id, type)
);
