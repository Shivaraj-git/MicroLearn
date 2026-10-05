-- Which student may open which course
CREATE TABLE course_access (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id    BIGINT    NOT NULL,
  course_id     BIGINT    NOT NULL,
  enrollment_id BIGINT    NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_student_course (student_id, course_id)
);

CREATE TABLE outbox_event (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  exchange_name VARCHAR(100)  NOT NULL,
  routing_key   VARCHAR(100)  NOT NULL,
  payload       VARCHAR(4000) NOT NULL,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at       TIMESTAMP     NULL
);
