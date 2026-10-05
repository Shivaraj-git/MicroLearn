package com.lms.progress.quiz;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "pass_mark")
    private int passMark;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<QuizQuestion> questions = new ArrayList<>();

    protected Quiz() {}

    public Quiz(Long courseId, int passMark, Long createdBy) {
        this.courseId = courseId;
        this.passMark = passMark;
        this.createdBy = createdBy;
    }

    /** Replaces all questions (used when the instructor saves the quiz again). */
    public void replaceWith(int passMark, List<QuizQuestion> newQuestions) {
        this.passMark = passMark;
        this.questions.clear();
        for (QuizQuestion q : newQuestions) {
            q.attachTo(this);
            this.questions.add(q);
        }
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public int getPassMark() { return passMark; }
    public List<QuizQuestion> getQuestions() { return questions; }
}
