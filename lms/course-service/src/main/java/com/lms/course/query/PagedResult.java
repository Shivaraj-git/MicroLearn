package com.lms.course.query;

import java.util.List;

public record PagedResult<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}
