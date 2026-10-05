package com.lms.course.command;

import java.util.List;

public record LearnView(Long courseId, String title, List<LessonContent> lessons) {
    public record LessonContent(int position, String title, String content) {}
}
