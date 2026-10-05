package com.lms.certificate;

import java.time.Instant;

public record CertificateResponse(String code, Long courseId, String courseTitle,
                                  String holder, Instant issuedAt) {

    /** For the student's own list: full email. */
    static CertificateResponse of(Certificate c) {
        return new CertificateResponse(c.getCode(), c.getCourseId(), c.getCourseTitle(),
                c.getStudentEmail(), c.getIssuedAt());
    }

    /** For public verification: email is partly hidden (s***@test.com). */
    static CertificateResponse publicView(Certificate c) {
        String email = c.getStudentEmail();
        int at = email.indexOf('@');
        String masked = at > 0 ? email.charAt(0) + "***" + email.substring(at) : "***";
        return new CertificateResponse(c.getCode(), c.getCourseId(), c.getCourseTitle(),
                masked, c.getIssuedAt());
    }
}
