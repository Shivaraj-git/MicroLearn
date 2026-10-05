package com.lms.progress;

import com.lms.progress.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final CourseAccessRepository access;
    private final ProgressService service;

    public ProgressController(CourseAccessRepository access, ProgressService service) {
        this.access = access;
        this.service = service;
    }

    /** Ids of the courses this student is allowed to open. */
    @GetMapping("/my-courses")
    public List<Long> myCourses(@AuthenticationPrincipal AuthUser user) {
        return access.findByStudentId(user.id()).stream().map(CourseAccess::getCourseId).toList();
    }

    /** How far is this student in this course? */
    @GetMapping("/courses/{courseId}")
    public CourseProgressResponse progress(@PathVariable Long courseId,
                                           @AuthenticationPrincipal AuthUser user) {
        return service.get(courseId, user);
    }

    /** Mark lesson number {position} as done. Finishing the last one completes the course. */
    @PostMapping("/courses/{courseId}/lessons/{position}/complete")
    public CourseProgressResponse complete(@PathVariable Long courseId, @PathVariable int position,
                                           @AuthenticationPrincipal AuthUser user) {
        return service.completeLesson(courseId, position, user);
    }
}
