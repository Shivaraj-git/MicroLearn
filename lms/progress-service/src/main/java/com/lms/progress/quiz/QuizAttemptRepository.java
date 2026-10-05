package com.lms.progress.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByQuizIdAndStudentIdOrderByIdDesc(Long quizId, Long studentId);
    boolean existsByQuizIdAndStudentIdAndStatus(Long quizId, Long studentId, AttemptStatus status);
}
