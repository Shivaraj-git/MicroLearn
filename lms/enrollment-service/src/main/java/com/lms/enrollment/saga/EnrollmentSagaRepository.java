package com.lms.enrollment.saga;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentSagaRepository extends JpaRepository<EnrollmentSaga, String> {}
