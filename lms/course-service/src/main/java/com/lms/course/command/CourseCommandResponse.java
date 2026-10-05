package com.lms.course.command;

public record CourseCommandResponse(Long courseId, String status, int lessonCount) {}
