package com.lms.enrollment;

import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.common.saga.ProcessPaymentCommand;
import com.lms.enrollment.common.ApiException;
import com.lms.enrollment.outbox.OutboxWriter;
import com.lms.enrollment.saga.EnrollmentSaga;
import com.lms.enrollment.saga.EnrollmentSagaRepository;
import com.lms.enrollment.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final EnrollmentSagaRepository sagas;
    private final OutboxWriter outbox;
    private final CatalogClient catalog;
    private final TransactionTemplate tx;

    public EnrollmentService(EnrollmentRepository enrollments, EnrollmentSagaRepository sagas,
                             OutboxWriter outbox, CatalogClient catalog,
                             PlatformTransactionManager txManager) {
        this.enrollments = enrollments;
        this.sagas = sagas;
        this.outbox = outbox;
        this.catalog = catalog;
        this.tx = new TransactionTemplate(txManager);
    }

    /** STEP 1 of the saga: save as PENDING and ask Payment to charge. */
    public EnrollmentResponse enroll(EnrollRequest req, AuthUser user) {
        CatalogItem course = catalog.find(req.courseId());   // HTTP call, outside the transaction

        return tx.execute(status -> {
            if (enrollments.existsByStudentIdAndCourseIdAndStatusIn(user.id(), course.courseId(),
                    List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED))) {
                throw new ApiException(HttpStatus.CONFLICT, "Already enrolled, or enrollment in progress");
            }

            Enrollment e = enrollments.save(new Enrollment(user.id(), course.courseId(), course.title(), course.price()));

            UUID sagaId = UUID.randomUUID();
            sagas.save(new EnrollmentSaga(sagaId.toString(), e.getId()));

            outbox.add(Exchanges.COMMANDS, RoutingKeys.PAYMENT_PROCESS,
                    new ProcessPaymentCommand(sagaId, e.getId(), user.id(),
                            course.price(), req.paymentMethod()));

            return EnrollmentResponse.of(e);
        });
    }

    public EnrollmentResponse get(Long id, AuthUser user) {
        Enrollment e = enrollments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        if (!e.getStudentId().equals(user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your enrollment");
        }
        return EnrollmentResponse.of(e);
    }

    public List<EnrollmentResponse> mine(AuthUser user) {
        return enrollments.findByStudentIdOrderByIdDesc(user.id())
                .stream().map(EnrollmentResponse::of).toList();
    }
}
