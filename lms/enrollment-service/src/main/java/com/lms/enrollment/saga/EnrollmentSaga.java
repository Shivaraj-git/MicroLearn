package com.lms.enrollment.saga;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollment_sagas")
public class EnrollmentSaga {

    @Id
    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Enumerated(EnumType.STRING)
    private SagaState state;

    @Version
    private Long version;   // protects against two messages changing the saga at once

    protected EnrollmentSaga() {}

    public EnrollmentSaga(String sagaId, Long enrollmentId) {
        this.sagaId = sagaId;
        this.enrollmentId = enrollmentId;
        this.state = SagaState.PAYMENT_PENDING;
    }

    public void awaitAccess() { this.state = SagaState.ACCESS_PENDING; }
    public void compensate()  { this.state = SagaState.COMPENSATING; }
    public void complete()    { this.state = SagaState.COMPLETED; }
    public void fail()        { this.state = SagaState.FAILED; }

    public Long getEnrollmentId() { return enrollmentId; }
    public SagaState getState() { return state; }
}
