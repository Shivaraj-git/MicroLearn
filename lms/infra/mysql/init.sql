-- One schema per service (database-per-service pattern)
CREATE DATABASE IF NOT EXISTS lms_identity;
CREATE DATABASE IF NOT EXISTS lms_course_write;
CREATE DATABASE IF NOT EXISTS lms_course_read;
CREATE DATABASE IF NOT EXISTS lms_enrollment;
CREATE DATABASE IF NOT EXISTS lms_payment;
CREATE DATABASE IF NOT EXISTS lms_progress;
CREATE DATABASE IF NOT EXISTS lms_certificate;
CREATE DATABASE IF NOT EXISTS lms_notification;

-- Dedicated app user (don't use root from services)
CREATE USER IF NOT EXISTS 'lms'@'%' IDENTIFIED BY 'lms';
GRANT ALL PRIVILEGES ON `lms\_%`.* TO 'lms'@'%';
FLUSH PRIVILEGES;
