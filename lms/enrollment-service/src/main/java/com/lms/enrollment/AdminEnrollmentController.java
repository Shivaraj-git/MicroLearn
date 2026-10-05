package com.lms.enrollment;

import com.lms.enrollment.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/enrollments")
public class AdminEnrollmentController {

    public record EnrollmentAdminView(Long id, Long studentId, Long courseId, String courseTitle,
                                      BigDecimal price, String status) {
        static EnrollmentAdminView of(Enrollment e) {
            return new EnrollmentAdminView(e.getId(), e.getStudentId(), e.getCourseId(),
                    e.getCourseTitle(), e.getPrice(), e.getStatus().name());
        }
    }

    public record EnrollmentPage(List<EnrollmentAdminView> items, int page, int size,
                                 long totalItems, int totalPages) {}

    private final EnrollmentRepository enrollments;

    public AdminEnrollmentController(EnrollmentRepository enrollments) { this.enrollments = enrollments; }

    /** GET /api/admin/enrollments?status=CONFIRMED&page=0&size=20   (all parameters optional) */
    @GetMapping
    public EnrollmentPage list(@RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));

        Page<Enrollment> result;
        if (status == null || status.isBlank()) {
            result = enrollments.findAll(pageRequest);
        } else {
            EnrollmentStatus parsed;
            try {
                parsed = EnrollmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Unknown status. Use PENDING, CONFIRMED, REJECTED or CANCELLED");
            }
            result = enrollments.findByStatus(parsed, pageRequest);
        }

        return new EnrollmentPage(result.getContent().stream().map(EnrollmentAdminView::of).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
