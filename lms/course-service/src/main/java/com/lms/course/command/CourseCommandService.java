package com.lms.course.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.course.common.ApiException;
import com.lms.course.event.CourseChangedEvent;
import com.lms.course.outbox.OutboxEvent;
import com.lms.course.outbox.OutboxRepository;
import com.lms.course.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class CourseCommandService {

    private final CourseRepository repo;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public CourseCommandService(CourseRepository repo, OutboxRepository outbox, ObjectMapper mapper) {
        this.repo = repo;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    // ===================== commands (change data) =====================

    @Transactional
    public CourseCommandResponse create(CreateCourseRequest req, AuthUser user) {
        Course c = repo.save(new Course(req.title(), req.description(), req.price(),
                user.id(), user.email(), user.name()));
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse update(Long courseId, UpdateCourseRequest req, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        c.update(req.title(), req.description(), req.price());
        if (c.isPublished()) {
            recordChange(c, false);   // refresh the catalog
        }
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse addLesson(Long courseId, AddLessonRequest req, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        c.addLesson(req.title(), req.content());
        if (c.isPublished()) {
            recordChange(c, false);   // lesson count changed
        }
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse updateLesson(Long courseId, Long lessonId,
                                              UpdateLessonRequest req, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        Lesson lesson = c.findLesson(lessonId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lesson not found"));
        lesson.update(req.title(), req.content());
        return toResponse(c);   // lesson text is not shown in the catalog, so no event needed
    }

    @Transactional
    public CourseCommandResponse removeLesson(Long courseId, Long lessonId, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        if (c.isPublished()) {
            // students may already have progress recorded against lesson numbers
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Unpublish the course before deleting lessons");
        }
        Lesson lesson = c.findLesson(lessonId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lesson not found"));
        c.removeLesson(lesson);
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse publish(Long courseId, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        try {
            c.publish();
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        recordChange(c, false);
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse unpublish(Long courseId, AuthUser user) {
        Course c = ownedCourse(courseId, user);
        try {
            c.unpublish();
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        recordChange(c, true);   // remove from the catalog
        return toResponse(c);
    }

    // ===================== admin =====================

    @Transactional(readOnly = true)
    public List<AdminCourseView> adminListAll() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "id")).stream().map(AdminCourseView::of).toList();
    }

    @Transactional
    public CourseCommandResponse suspend(Long courseId) {
        Course c = findAny(courseId);
        try {
            c.suspend();
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        recordChange(c, true);   // remove from the catalog
        return toResponse(c);
    }

    @Transactional
    public CourseCommandResponse reinstate(Long courseId) {
        Course c = findAny(courseId);
        try {
            c.reinstate();
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return toResponse(c);    // back to DRAFT, nothing to announce yet
    }

    private Course findAny(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    // ===================== instructor's own view (reads the WRITE database,
    // because drafts are not in the catalog) =====================

    @Transactional(readOnly = true)
    public List<CourseSummary> myCourses(AuthUser user) {
        return repo.findByInstructorIdOrderByIdDesc(user.id()).stream().map(CourseSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public CourseDetail detail(Long courseId, AuthUser user) {
        return CourseDetail.of(ownedCourse(courseId, user));
    }

    // ===================== student: lesson content =====================

    @Transactional(readOnly = true)
    public LearnView learnView(Long courseId) {
        Course c = findAny(courseId);
        if (!c.isPublished()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This course is not available right now");
        }
        return new LearnView(c.getId(), c.getTitle(), c.getLessons().stream()
                .map(l -> new LearnView.LessonContent(l.getPosition(), l.getTitle(), l.getContent()))
                .toList());
    }

    // ===================== helpers =====================

    /** Saves the message in the outbox table, in the SAME transaction as the course change. */
    private void recordChange(Course c, boolean removed) {
        try {
            CourseChangedEvent event = new CourseChangedEvent(c.getId(), c.getTitle(),
                    c.getDescription(), c.getPrice(), c.getInstructorEmail(), c.getInstructorName(),
                    c.getLessons().size(), removed);
            outbox.save(new OutboxEvent("course.changed", mapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private Course ownedCourse(Long id, AuthUser user) {
        Course c = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!c.getInstructorId().equals(user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your course");
        }
        c.refreshInstructorName(user.name());
        return c;
    }

    private CourseCommandResponse toResponse(Course c) {
        return new CourseCommandResponse(c.getId(), c.getStatus().name(), c.getLessons().size());
    }
}
