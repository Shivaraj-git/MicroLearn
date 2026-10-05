package com.lms.progress.quiz;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "score_percent")
    private int scorePercent;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected QuizAttempt() {}

    public QuizAttempt(Long quizId, Long studentId, int scorePercent, AttemptStatus status) {
        this.quizId = quizId;
        this.studentId = studentId;
        this.scorePercent = scorePercent;
        this.status = status;
    }

    public Long getId() { return id; }
    public int getScorePercent() { return scorePercent; }
    public AttemptStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
