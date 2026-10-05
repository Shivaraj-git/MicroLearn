package com.lms.progress.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** All the small request/response shapes for quizzes, in one place. */
public final class QuizDtos {
    private QuizDtos() {}

    // ---- instructor creates a quiz ----
    public record OptionRequest(@NotBlank String text, boolean correct) {}
    public record QuestionRequest(@NotBlank String text, @NotEmpty @Valid List<OptionRequest> options) {}
    public record CreateQuizRequest(@Min(1) @Max(100) int passMark,
                                    @NotEmpty @Valid List<QuestionRequest> questions) {}

    // ---- what a STUDENT sees: no answers ----
    public record OptionView(Long id, String text) {}
    public record QuestionView(Long id, String text, List<OptionView> options) {}
    public record QuizView(Long quizId, int passMark, List<QuestionView> questions) {}

    // ---- what the INSTRUCTOR sees: with the correct answers ----
    public record OptionManageView(Long id, String text, boolean correct) {}
    public record QuestionManageView(Long id, String text, List<OptionManageView> options) {}
    public record QuizManageView(Long quizId, int passMark, List<QuestionManageView> questions) {}

    // ---- student answers ----
    public record AnswerRequest(Long questionId, Long optionId) {}
    public record SubmitAttemptRequest(@NotNull List<AnswerRequest> answers) {}

    // ---- results ----
    public record AttemptResult(Long attemptId, int scorePercent, int passMark, boolean passed,
                                int correctAnswers, int totalQuestions) {}
    public record AttemptView(Long attemptId, int scorePercent, String status, Instant createdAt) {}
}
