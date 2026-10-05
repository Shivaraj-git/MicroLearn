package com.lms.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saga_id")
    private String sagaId;

    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected Payment() {}

    public Payment(String sagaId, Long enrollmentId, Long studentId, BigDecimal amount,
                   String paymentMethod, PaymentStatus status) {
        this.sagaId = sagaId;
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public PaymentStatus getStatus() { return status; }

    public void refund() { this.status = PaymentStatus.REFUNDED; }
}
