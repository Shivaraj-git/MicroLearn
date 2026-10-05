CREATE TABLE certificates (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  certificate_code VARCHAR(36)  NOT NULL UNIQUE,
  student_id       BIGINT       NOT NULL,
  student_email    VARCHAR(150) NOT NULL,
  course_id        BIGINT       NOT NULL,
  course_title     VARCHAR(200) NOT NULL,
  issued_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- one certificate per student per course, even if the event arrives twice
  UNIQUE KEY uq_student_course (student_id, course_id)
);
