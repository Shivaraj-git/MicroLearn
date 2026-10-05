package com.lms.course.command;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    private final CourseCommandService service;

    public AdminCourseController(CourseCommandService service) { this.service = service; }

    /** Every course, whatever its status. */
    @GetMapping
    public List<AdminCourseView> list() { return service.adminListAll(); }

    /** Take a published course off the catalog. The instructor cannot re-publish it. */
    @PostMapping("/{id}/suspend")
    public CourseCommandResponse suspend(@PathVariable Long id) { return service.suspend(id); }

    /** Lets the instructor publish it again (it comes back as a DRAFT). */
    @PostMapping("/{id}/reinstate")
    public CourseCommandResponse reinstate(@PathVariable Long id) { return service.reinstate(id); }
}
