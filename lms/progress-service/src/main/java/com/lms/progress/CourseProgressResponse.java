package com.lms.progress;

import java.util.List;

public record CourseProgressResponse(Long courseId, List<Integer> completedLessons,
                                     int totalLessons, boolean courseCompleted) {}
