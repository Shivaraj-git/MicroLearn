CREATE TABLE lesson_progress (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id   BIGINT    NOT NULL,
  course_id    BIGINT    NOT NULL,
  position     INT       NOT NULL,          -- lesson number inside the course (1, 2, 3...)
  completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_student_course_lesson (student_id, course_id, position)
);

ALTER TABLE course_access ADD COLUMN completed_at TIMESTAMP NULL;
