package com.lms.certificate;

import com.lms.certificate.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateRepository repo;

    public CertificateController(CertificateRepository repo) { this.repo = repo; }

    /** The logged-in student's certificates. */
    @GetMapping("/my")
    public List<CertificateResponse> mine(@AuthenticationPrincipal AuthUser user) {
        return repo.findByStudentIdOrderByIdDesc(user.id())
                .stream().map(CertificateResponse::of).toList();
    }

    /** PUBLIC: anyone (an employer, for example) can check a certificate code. */
    @GetMapping("/verify/{code}")
    public ResponseEntity<?> verify(@PathVariable String code) {
        return repo.findByCode(code)
                .<ResponseEntity<?>>map(c -> ResponseEntity.ok(CertificateResponse.publicView(c)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Certificate not found")));
    }
}
