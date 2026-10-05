package com.lms.course.command;

import com.lms.course.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseCommandController {

    private final CourseCommandService service;

    public CourseCommandController(CourseCommandService service) { this.service = service; }

    // ---- create / edit ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseCommandResponse create(@Valid @RequestBody CreateCourseRequest req,
                                        @AuthenticationPrincipal AuthUser user) {
        return service.create(req, user);
    }

    @PutMapping("/{id}")
    public CourseCommandResponse update(@PathVariable Long id,
                                        @Valid @RequestBody UpdateCourseRequest req,
                                        @AuthenticationPrincipal AuthUser user) {
        return service.update(id, req, user);
    }

    // ---- lessons ----

    @PostMapping("/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseCommandResponse addLesson(@PathVariable Long id,
                                           @Valid @RequestBody AddLessonRequest req,
                                           @AuthenticationPrincipal AuthUser user) {
        return service.addLesson(id, req, user);
    }

    @PutMapping("/{id}/lessons/{lessonId}")
    public CourseCommandResponse updateLesson(@PathVariable Long id, @PathVariable Long lessonId,
                                              @Valid @RequestBody UpdateLessonRequest req,
                                              @AuthenticationPrincipal AuthUser user) {
        return service.updateLesson(id, lessonId, req, user);
    }

    @DeleteMapping("/{id}/lessons/{lessonId}")
    public CourseCommandResponse removeLesson(@PathVariable Long id, @PathVariable Long lessonId,
                                              @AuthenticationPrincipal AuthUser user) {
        return service.removeLesson(id, lessonId, user);
    }

    // ---- publish / unpublish ----

    @PostMapping("/{id}/publish")
    public CourseCommandResponse publish(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthUser user) {
        return service.publish(id, user);
    }

    @PostMapping("/{id}/unpublish")
    public CourseCommandResponse unpublish(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthUser user) {
        return service.unpublish(id, user);
    }

    // ---- instructor's own courses (drafts included) ----

    @GetMapping("/mine")
    public List<CourseSummary> mine(@AuthenticationPrincipal AuthUser user) {
        return service.myCourses(user);
    }

    @GetMapping("/{id}")
    public CourseDetail detail(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return service.detail(id, user);
    }
}
