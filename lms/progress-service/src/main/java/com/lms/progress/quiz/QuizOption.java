package com.lms.progress.quiz;

import jakarta.persistence.*;

@Entity
@Table(name = "quiz_options")
public class QuizOption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    @Column(name = "option_text")
    private String text;

    @Column(name = "is_correct")
    private int correct;   // 1 or 0

    protected QuizOption() {}

    QuizOption(QuizQuestion question, String text, boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct ? 1 : 0;
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct == 1; }
}
