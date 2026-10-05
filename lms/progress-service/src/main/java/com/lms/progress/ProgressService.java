package com.lms.progress;

import com.lms.common.event.CourseCompletedEvent;
import com.lms.common.messaging.Exchanges;
import com.lms.common.messaging.RoutingKeys;
import com.lms.progress.common.ApiException;
import com.lms.progress.outbox.OutboxWriter;
import com.lms.progress.quiz.AttemptStatus;
import com.lms.progress.quiz.Quiz;
import com.lms.progress.quiz.QuizAttemptRepository;
import com.lms.progress.quiz.QuizRepository;
import com.lms.progress.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class ProgressService {

    private final CourseAccessRepository access;
    private final LessonProgressRepository lessons;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final CatalogClient catalog;
    private final OutboxWriter outbox;
    private final TransactionTemplate tx;

    public ProgressService(CourseAccessRepository access, LessonProgressRepository lessons,
                           QuizRepository quizzes, QuizAttemptRepository attempts,
                           CatalogClient catalog, OutboxWriter outbox,
                           PlatformTransactionManager txManager) {
        this.access = access;
        this.lessons = lessons;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.catalog = catalog;
        this.outbox = outbox;
        this.tx = new TransactionTemplate(txManager);
    }

    public CourseProgressResponse completeLesson(Long courseId, int position, AuthUser user) {
        requireAccess(courseId, user);
        CatalogItem course = catalog.find(courseId);   // HTTP call, outside the transaction

        if (position < 1 || position > course.lessonCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Lesson number must be between 1 and " + course.lessonCount());
        }

        return tx.execute(status -> {
            CourseAccess a = requireAccess(courseId, user);

            if (!lessons.existsByStudentIdAndCourseIdAndPosition(user.id(), courseId, position)) {
                lessons.save(new LessonProgress(user.id(), courseId, position));
            }

            List<Integer> done = completedPositions(user.id(), courseId);
            completeIfReady(a, user, course, done);
            return new CourseProgressResponse(courseId, done, course.lessonCount(), a.isCompleted());
        });
    }

    /** Called after a quiz is passed: maybe that was the last missing piece. */
    public void recheckCompletion(Long courseId, AuthUser user) {
        CatalogItem course = catalog.find(courseId);
        tx.executeWithoutResult(status -> {
            CourseAccess a = requireAccess(courseId, user);
            completeIfReady(a, user, course, completedPositions(user.id(), courseId));
        });
    }

    public CourseProgressResponse get(Long courseId, AuthUser user) {
        CourseAccess a = requireAccess(courseId, user);
        CatalogItem course = catalog.find(courseId);
        return new CourseProgressResponse(courseId, completedPositions(user.id(), courseId),
                course.lessonCount(), a.isCompleted());
    }

    /**
     * A course is completed when ALL lessons are done AND, if the course has a quiz,
     * the student has passed it. Then we announce it (once) so Certificate can react.
     */
    private void completeIfReady(CourseAccess a, AuthUser user, CatalogItem course, List<Integer> done) {
        if (a.isCompleted()) return;
        if (done.size() < course.lessonCount()) return;

        Quiz quiz = quizzes.findByCourseId(course.courseId()).orElse(null);
        if (quiz != null && !attempts.existsByQuizIdAndStudentIdAndStatus(
                quiz.getId(), user.id(), AttemptStatus.PASSED)) {
            return;   // lessons done, but the quiz is not passed yet
        }

        a.markCompleted();
        outbox.add(Exchanges.EVENTS, RoutingKeys.COURSE_COMPLETED,
                new CourseCompletedEvent(user.id(), user.email(), course.courseId(), course.title()));
    }

    private CourseAccess requireAccess(Long courseId, AuthUser user) {
        return access.findByStudentIdAndCourseId(user.id(), courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You do not have access to this course"));
    }

    private List<Integer> completedPositions(Long studentId, Long courseId) {
        return lessons.findByStudentIdAndCourseIdOrderByPositionAsc(studentId, courseId)
                .stream().map(LessonProgress::getPosition).toList();
    }
}
