package com.lms.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    boolean existsByStudentIdAndCourseIdAndPosition(Long studentId, Long courseId, int position);
    List<LessonProgress> findByStudentIdAndCourseIdOrderByPositionAsc(Long studentId, Long courseId);
}
