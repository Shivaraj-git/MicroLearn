package com.lms.course.command;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "courses")
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "instructor_id")
    private Long instructorId;

    @Column(name = "instructor_email")
    private String instructorEmail;

    @Column(name = "instructor_name")
    private String instructorName;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<Lesson> lessons = new ArrayList<>();

    protected Course() {}

    public Course(String title, String description, BigDecimal price,
                  Long instructorId, String instructorEmail, String instructorName) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.instructorId = instructorId;
        this.instructorEmail = instructorEmail;
        this.instructorName = instructorName;
        this.status = CourseStatus.DRAFT;
    }

    public void update(String title, String description, BigDecimal price) {
        this.title = title;
        this.description = description;
        this.price = price;
    }

    public void addLesson(String title, String content) {
        lessons.add(new Lesson(this, title, content, lessons.size() + 1));
    }

    public Optional<Lesson> findLesson(Long lessonId) {
        return lessons.stream().filter(l -> l.getId().equals(lessonId)).findFirst();
    }

    /** Removes a lesson and renumbers the rest so positions stay 1, 2, 3... */
    public void removeLesson(Lesson lesson) {
        lessons.remove(lesson);
        for (int i = 0; i < lessons.size(); i++) {
            lessons.get(i).moveTo(i + 1);
        }
    }

    public void publish() {
        if (status == CourseStatus.SUSPENDED) {
            throw new IllegalStateException("This course is suspended by an admin");
        }
        if (status == CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Course is already published");
        }
        if (lessons.isEmpty()) {
            throw new IllegalStateException("Add at least one lesson before publishing");
        }
        this.status = CourseStatus.PUBLISHED;
    }

    public void unpublish() {
        if (status != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Course is not published");
        }
        this.status = CourseStatus.DRAFT;
    }

    public void suspend() {
        if (status != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Only published courses can be suspended");
        }
        this.status = CourseStatus.SUSPENDED;
    }

    /** After reinstating, the course is a DRAFT: the instructor publishes it again. */
    public void reinstate() {
        if (status != CourseStatus.SUSPENDED) {
            throw new IllegalStateException("Course is not suspended");
        }
        this.status = CourseStatus.DRAFT;
    }

    public boolean isPublished() { return status == CourseStatus.PUBLISHED; }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Long getInstructorId() { return instructorId; }
    public String getInstructorEmail() { return instructorEmail; }
    public String getInstructorName() { return instructorName; }

    /** Old courses have no name yet; the owner's next action fills it in. */
    public void refreshInstructorName(String name) {
        if (name != null && !name.isBlank()) this.instructorName = name;
    }
    public CourseStatus getStatus() { return status; }
    public List<Lesson> getLessons() { return lessons; }
}
