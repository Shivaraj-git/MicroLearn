package com.lms.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseAccessRepository extends JpaRepository<CourseAccess, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    List<CourseAccess> findByStudentId(Long studentId);
    Optional<CourseAccess> findByStudentIdAndCourseId(Long studentId, Long courseId);
}
