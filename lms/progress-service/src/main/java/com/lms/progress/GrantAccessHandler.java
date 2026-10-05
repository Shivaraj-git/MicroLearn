package com.lms.progress;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.common.saga.GrantAccessCommand;
import com.lms.common.saga.ReplyTypes;
import com.lms.common.saga.SagaReply;
import com.lms.progress.config.RabbitConfig;
import com.lms.progress.outbox.OutboxWriter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GrantAccessHandler {

    private final CourseAccessRepository access;
    private final OutboxWriter outbox;
    private final FailureSwitch failureSwitch;

    public GrantAccessHandler(CourseAccessRepository access, OutboxWriter outbox,
                              FailureSwitch failureSwitch) {
        this.access = access;
        this.outbox = outbox;
        this.failureSwitch = failureSwitch;
    }

    @RabbitListener(queues = RabbitConfig.COMMAND_QUEUE)
    @Transactional
    public void on(GrantAccessCommand cmd) {
        SagaReply reply;

        if (failureSwitch.consume()) {
            reply = new SagaReply(cmd.sagaId(), ReplyTypes.ACCESS_GRANT_FAILED, "Simulated failure");
        } else {
            // if this message arrives twice, we don't create a second row
            if (!access.existsByStudentIdAndCourseId(cmd.studentId(), cmd.courseId())) {
                access.save(new CourseAccess(cmd.studentId(), cmd.courseId(), cmd.enrollmentId()));
            }
            reply = new SagaReply(cmd.sagaId(), ReplyTypes.ACCESS_GRANTED, null);
        }

        outbox.add(Exchanges.EVENTS, RoutingKeys.ENROLLMENT_SAGA_REPLY, reply);
    }
}
