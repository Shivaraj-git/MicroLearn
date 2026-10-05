package com.lms.course.command;

import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private String title;
    @Column(columnDefinition = "mediumtext")
    private String content;
    private int position;

    protected Lesson() {}

    Lesson(Course course, String title, String content, int position) {
        this.course = course;
        this.title = title;
        this.content = content;
        this.position = position;
    }

    void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    void moveTo(int position) { this.position = position; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getPosition() { return position; }
}
