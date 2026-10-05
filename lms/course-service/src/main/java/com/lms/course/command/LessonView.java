package com.lms.course.command;

public record LessonView(Long id, String title, String content, int position) {

    static LessonView of(Lesson l) {
        return new LessonView(l.getId(), l.getTitle(), l.getContent(), l.getPosition());
    }
}
