package com.lms.course.query;

import com.lms.course.config.RabbitConfig;
import com.lms.course.event.CourseChangedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Receives messages from RabbitMQ and keeps the read database (the catalog) up to date. */
@Component
public class CatalogProjector {

    private final JdbcTemplate jdbc;

    public CatalogProjector(@Qualifier("readJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @RabbitListener(queues = RabbitConfig.PROJECTOR_QUEUE)
    public void on(CourseChangedEvent e) {
        if (e.removed()) {
            jdbc.update("DELETE FROM course_catalog_view WHERE course_id = ?", e.courseId());
            return;
        }
        jdbc.update("""
            INSERT INTO course_catalog_view
              (course_id, title, description, price, instructor_email, instructor_name, lesson_count)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              title = VALUES(title), description = VALUES(description), price = VALUES(price),
              instructor_email = VALUES(instructor_email),
              instructor_name = COALESCE(VALUES(instructor_name), instructor_name),
              lesson_count = VALUES(lesson_count)
            """,
            e.courseId(), e.title(), e.description(), e.price(), e.instructorEmail(),
            e.instructorName(), e.lessonCount());
    }
}
