package com.lms.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudentIdOrderByIdDesc(Long studentId);
    Optional<Certificate> findByCode(String code);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
