package com.lms.common.event;

/** Published by Identity when a user changes their name. */
public record UserRenamedEvent(Long userId, String email, String name) {}
