-- Flat, ready-to-read table. No joins needed to show the catalog.
CREATE TABLE course_catalog_view (
  course_id        BIGINT PRIMARY KEY,
  title            VARCHAR(200)  NOT NULL,
  description      VARCHAR(1000),
  price            DECIMAL(10,2) NOT NULL,
  instructor_email VARCHAR(150)  NOT NULL,
  lesson_count     INT           NOT NULL,
  updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
