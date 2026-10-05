package com.lms.course.command;

import com.lms.course.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lesson text for students who have access to the course. */
@RestController
@RequestMapping("/api/learn/courses")
public class LearnController {

    private final CourseCommandService service;
    private final AccessClient access;

    public LearnController(CourseCommandService service, AccessClient access) {
        this.service = service;
        this.access = access;
    }

    @GetMapping("/{courseId}")
    public LearnView lessons(@PathVariable Long courseId,
                             @RequestHeader("Authorization") String authorization) {
        if (!access.hasAccess(courseId, authorization)) {   // HTTP call, outside any transaction
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this course");
        }
        return service.learnView(courseId);
    }
}
