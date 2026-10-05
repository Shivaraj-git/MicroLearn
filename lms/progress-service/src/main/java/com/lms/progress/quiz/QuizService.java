package com.lms.progress.quiz;

import com.lms.progress.CourseAccessRepository;
import com.lms.progress.ProgressService;
import com.lms.progress.common.ApiException;
import com.lms.progress.quiz.QuizDtos.*;
import com.lms.progress.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final CourseAccessRepository access;
    private final CourseOwnerClient owner;
    private final ProgressService progress;
    private final TransactionTemplate tx;

    public QuizService(QuizRepository quizzes, QuizAttemptRepository attempts,
                       CourseAccessRepository access, CourseOwnerClient owner,
                       ProgressService progress, PlatformTransactionManager txManager) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.access = access;
        this.owner = owner;
        this.progress = progress;
        this.tx = new TransactionTemplate(txManager);
    }

    // ===================== instructor =====================

    /** Creates the quiz for a course, or replaces it if one already exists. */
    public QuizManageView save(Long courseId, CreateQuizRequest req, AuthUser user, String authHeader) {
        owner.requireOwner(courseId, authHeader);          // HTTP call, outside the transaction
        List<QuizQuestion> built = buildQuestions(req);

        return tx.execute(status -> {
            Quiz quiz = quizzes.findByCourseId(courseId)
                    .orElseGet(() -> new Quiz(courseId, req.passMark(), user.id()));
            quiz.replaceWith(req.passMark(), built);
            return toManageView(quizzes.saveAndFlush(quiz));   // flush so new ids exist
        });
    }

    /** The instructor's view, including which answers are correct. */
    public QuizManageView manage(Long courseId, String authHeader) {
        owner.requireOwner(courseId, authHeader);
        return tx.execute(status -> toManageView(findQuiz(courseId)));
    }

    // ===================== student =====================

    /** The quiz WITHOUT the correct answers. */
    public QuizView forStudent(Long courseId, AuthUser user) {
        requireAccess(courseId, user);
        return tx.execute(status -> {
            Quiz quiz = findQuiz(courseId);
            List<QuestionView> questions = quiz.getQuestions().stream()
                    .map(q -> new QuestionView(q.getId(), q.getText(),
                            q.getOptions().stream().map(o -> new OptionView(o.getId(), o.getText())).toList()))
                    .toList();
            return new QuizView(quiz.getId(), quiz.getPassMark(), questions);
        });
    }

    /** Grades the answers, saves the attempt, and may complete the course. */
    public AttemptResult submit(Long courseId, SubmitAttemptRequest req, AuthUser user) {
        requireAccess(courseId, user);

        AttemptResult result = tx.execute(status -> {
            Quiz quiz = findQuiz(courseId);

            Map<Long, Long> chosen = new HashMap<>();   // questionId -> optionId
            for (AnswerRequest a : req.answers()) {
                if (a.questionId() != null) chosen.put(a.questionId(), a.optionId());
            }

            int correct = 0;
            for (QuizQuestion q : quiz.getQuestions()) {
                Long picked = chosen.get(q.getId());
                boolean right = picked != null && q.getOptions().stream()
                        .anyMatch(o -> o.getId().equals(picked) && o.isCorrect());
                if (right) correct++;
            }

            int total = quiz.getQuestions().size();
            int score = (int) Math.round(correct * 100.0 / total);
            boolean passed = score >= quiz.getPassMark();

            QuizAttempt attempt = attempts.save(new QuizAttempt(quiz.getId(), user.id(), score,
                    passed ? AttemptStatus.PASSED : AttemptStatus.FAILED));
            return new AttemptResult(attempt.getId(), score, quiz.getPassMark(), passed, correct, total);
        });

        if (result.passed()) {
            try {
                progress.recheckCompletion(courseId, user);   // passing may finish the course
            } catch (ApiException e) {
                log.warn("Quiz passed but could not re-check course completion: {}", e.getMessage());
            }
        }
        return result;
    }

    public List<AttemptView> myAttempts(Long courseId, AuthUser user) {
        requireAccess(courseId, user);
        return tx.execute(status -> {
            Quiz quiz = findQuiz(courseId);
            return attempts.findByQuizIdAndStudentIdOrderByIdDesc(quiz.getId(), user.id()).stream()
                    .map(a -> new AttemptView(a.getId(), a.getScorePercent(),
                            a.getStatus().name(), a.getCreatedAt()))
                    .toList();
        });
    }

    // ===================== helpers =====================

    private Quiz findQuiz(Long courseId) {
        return quizzes.findByCourseId(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This course has no quiz"));
    }

    private void requireAccess(Long courseId, AuthUser user) {
        if (!access.existsByStudentIdAndCourseId(user.id(), courseId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this course");
        }
    }

    /** Every question: 2 to 6 options, exactly one correct. */
    private List<QuizQuestion> buildQuestions(CreateQuizRequest req) {
        List<QuizQuestion> list = new ArrayList<>();
        int position = 1;
        for (QuestionRequest qr : req.questions()) {
            int optionCount = qr.options().size();
            if (optionCount < 2 || optionCount > 6) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each question needs 2 to 6 options");
            }
            long correctCount = qr.options().stream().filter(OptionRequest::correct).count();
            if (correctCount != 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Each question needs exactly one correct option");
            }
            QuizQuestion question = new QuizQuestion(qr.text(), position++);
            qr.options().forEach(o -> question.addOption(o.text(), o.correct()));
            list.add(question);
        }
        return list;
    }

    private QuizManageView toManageView(Quiz quiz) {
        List<QuestionManageView> questions = quiz.getQuestions().stream()
                .map(q -> new QuestionManageView(q.getId(), q.getText(),
                        q.getOptions().stream()
                                .map(o -> new OptionManageView(o.getId(), o.getText(), o.isCorrect()))
                                .toList()))
                .toList();
        return new QuizManageView(quiz.getId(), quiz.getPassMark(), questions);
    }
}
