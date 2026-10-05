package com.lms.certificate;

import com.lms.certificate.config.RabbitConfig;
import com.lms.common.event.CourseCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Issues a certificate whenever Progress announces that a course was completed. */
@Component
public class CourseCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(CourseCompletedListener.class);

    private final CertificateRepository repo;

    public CourseCompletedListener(CertificateRepository repo) { this.repo = repo; }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    @Transactional
    public void on(CourseCompletedEvent e) {
        if (repo.existsByStudentIdAndCourseId(e.studentId(), e.courseId())) {
            return;   // duplicate message, certificate already issued
        }
        Certificate c = repo.save(new Certificate(UUID.randomUUID().toString(),
                e.studentId(), e.studentEmail(), e.courseId(), e.courseTitle()));
        log.info("Certificate {} issued to {} for course \"{}\"",
                c.getCode(), e.studentEmail(), e.courseTitle());
    }
}
