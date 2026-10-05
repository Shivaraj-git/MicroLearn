package com.lms.course.profile;

import com.lms.common.event.UserRenamedEvent;
import com.lms.course.command.CourseRepository;
import com.lms.course.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** When someone renames themselves in Identity, update our copy of their name. */
@Component
public class UserRenamedListener {

    private final CourseRepository courses;
    private final JdbcTemplate readJdbc;

    public UserRenamedListener(CourseRepository courses, @Qualifier("readJdbc") JdbcTemplate readJdbc) {
        this.courses = courses;
        this.readJdbc = readJdbc;
    }

    @RabbitListener(queues = RabbitConfig.RENAME_QUEUE)
    @Transactional
    public void on(UserRenamedEvent e) {
        // write side: every course this person teaches (repeating the message is harmless)
        courses.findByInstructorIdOrderByIdDesc(e.userId()).forEach(c -> c.refreshInstructorName(e.name()));
        // read side: the catalog rows
        readJdbc.update("UPDATE course_catalog_view SET instructor_name = ? WHERE instructor_email = ?",
                e.name(), e.email());
    }
}
