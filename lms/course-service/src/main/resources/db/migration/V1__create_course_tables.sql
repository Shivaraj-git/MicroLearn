CREATE TABLE courses (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  title            VARCHAR(200)  NOT NULL,
  description      VARCHAR(1000),
  price            DECIMAL(10,2) NOT NULL,
  instructor_id    BIGINT        NOT NULL,
  instructor_email VARCHAR(150)  NOT NULL,
  status           VARCHAR(20)   NOT NULL,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lessons (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT        NOT NULL,
  title     VARCHAR(200)  NOT NULL,
  content   VARCHAR(4000),
  position  INT           NOT NULL,
  FOREIGN KEY (course_id) REFERENCES courses(id)
);
