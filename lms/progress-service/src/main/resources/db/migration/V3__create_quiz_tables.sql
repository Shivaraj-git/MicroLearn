CREATE TABLE quizzes (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id  BIGINT    NOT NULL UNIQUE,          -- one quiz per course
  pass_mark  INT       NOT NULL,                 -- percent needed to pass
  created_by BIGINT    NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quiz_questions (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id       BIGINT       NOT NULL,
  question_text VARCHAR(500) NOT NULL,
  position      INT          NOT NULL,
  FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

CREATE TABLE quiz_options (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT       NOT NULL,
  option_text VARCHAR(300) NOT NULL,
  is_correct  INT          NOT NULL,             -- 1 = the right answer, 0 = wrong
  FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE
);

CREATE TABLE quiz_attempts (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  quiz_id       BIGINT      NOT NULL,
  student_id    BIGINT      NOT NULL,
  score_percent INT         NOT NULL,
  status        VARCHAR(10) NOT NULL,            -- PASSED / FAILED
  created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_attempt_student_quiz (student_id, quiz_id)
);
