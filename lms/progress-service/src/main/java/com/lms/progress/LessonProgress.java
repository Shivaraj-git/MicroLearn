package com.lms.progress;

import jakarta.persistence.*;

@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "course_id")
    private Long courseId;

    private int position;

    protected LessonProgress() {}

    public LessonProgress(Long studentId, Long courseId, int position) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.position = position;
    }

    public int getPosition() { return position; }
}
