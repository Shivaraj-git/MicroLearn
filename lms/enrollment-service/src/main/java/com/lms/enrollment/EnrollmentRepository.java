package com.lms.enrollment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentIdOrderByIdDesc(Long studentId);
    boolean existsByStudentIdAndCourseIdAndStatusIn(Long studentId, Long courseId,
                                                    Collection<EnrollmentStatus> statuses);
    Page<Enrollment> findByStatus(EnrollmentStatus status, Pageable pageable);
}
