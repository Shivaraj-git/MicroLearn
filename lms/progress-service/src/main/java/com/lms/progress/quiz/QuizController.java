package com.lms.progress.quiz;

import com.lms.progress.quiz.QuizDtos.*;
import com.lms.progress.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes/courses")
public class QuizController {

    private final QuizService service;

    public QuizController(QuizService service) { this.service = service; }

    // ---- instructor ----

    /** Create or replace the quiz of a course. */
    @PostMapping("/{courseId}")
    public QuizManageView save(@PathVariable Long courseId,
                               @Valid @RequestBody CreateQuizRequest req,
                               @AuthenticationPrincipal AuthUser user,
                               @RequestHeader("Authorization") String authorization) {
        return service.save(courseId, req, user, authorization);
    }

    /** Quiz with the correct answers shown. */
    @GetMapping("/{courseId}/manage")
    public QuizManageView manage(@PathVariable Long courseId,
                                 @RequestHeader("Authorization") String authorization) {
        return service.manage(courseId, authorization);
    }

    // ---- student ----

    /** The quiz without answers. */
    @GetMapping("/{courseId}")
    public QuizView view(@PathVariable Long courseId, @AuthenticationPrincipal AuthUser user) {
        return service.forStudent(courseId, user);
    }

    @PostMapping("/{courseId}/attempts")
    public AttemptResult submit(@PathVariable Long courseId,
                                @Valid @RequestBody SubmitAttemptRequest req,
                                @AuthenticationPrincipal AuthUser user) {
        return service.submit(courseId, req, user);
    }

    @GetMapping("/{courseId}/attempts")
    public List<AttemptView> attempts(@PathVariable Long courseId, @AuthenticationPrincipal AuthUser user) {
        return service.myAttempts(courseId, user);
    }
}
