package com.lms.progress.quiz;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "question_text")
    private String text;

    private int position;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<QuizOption> options = new ArrayList<>();

    protected QuizQuestion() {}

    public QuizQuestion(String text, int position) {
        this.text = text;
        this.position = position;
    }

    void attachTo(Quiz quiz) { this.quiz = quiz; }

    public void addOption(String text, boolean correct) {
        options.add(new QuizOption(this, text, correct));
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public int getPosition() { return position; }
    public List<QuizOption> getOptions() { return options; }
}
