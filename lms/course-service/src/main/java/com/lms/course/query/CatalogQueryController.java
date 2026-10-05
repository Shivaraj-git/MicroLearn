package com.lms.course.query;

import com.lms.course.common.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/** Reads ONLY from the read database. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogQueryController {

    private static final RowMapper<CatalogItem> MAPPER = (rs, i) -> new CatalogItem(
            rs.getLong("course_id"), rs.getString("title"), rs.getString("description"),
            rs.getBigDecimal("price"), rs.getString("instructor_email"), rs.getInt("lesson_count"), rs.getString("instructor_name"));

    private final JdbcTemplate jdbc;

    public CatalogQueryController(@Qualifier("readJdbc") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** GET /api/catalog?q=java&page=0&size=10   (all parameters optional) */
    @GetMapping
    public PagedResult<CatalogItem> search(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        if (page < 0) page = 0;
        size = Math.min(Math.max(size, 1), 50);

        String where = "";
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String like = "%" + escapeLike(q.trim()) + "%";
            where = " WHERE title LIKE ? OR description LIKE ? OR instructor_email LIKE ? OR instructor_name LIKE ?";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }

        Long counted = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_catalog_view" + where, Long.class, args.toArray());
        long total = counted == null ? 0 : counted;

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(page * size);
        List<CatalogItem> items = jdbc.query(
                "SELECT * FROM course_catalog_view" + where + " ORDER BY course_id DESC LIMIT ? OFFSET ?",
                MAPPER, pageArgs.toArray());

        int totalPages = (int) Math.ceil(total / (double) size);
        return new PagedResult<>(items, page, size, total, totalPages);
    }

    @GetMapping("/{id}")
    public CatalogItem get(@PathVariable Long id) {
        List<CatalogItem> found = jdbc.query(
                "SELECT * FROM course_catalog_view WHERE course_id = ?", MAPPER, id);
        if (found.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "Course not found");
        return found.get(0);
    }

    /** So a search for "50%" does not act as a wildcard. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
