package com.lms.course.command;

import com.lms.course.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** Asks Progress which courses this student may open, forwarding the student's own token. */
@Component
public class AccessClient {

    private final RestClient client;

    public AccessClient(@Value("${app.progress-service-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean hasAccess(Long courseId, String authorizationHeader) {
        try {
            List<Long> ids = client.get().uri("/api/progress/my-courses")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve().body(new ParameterizedTypeReference<List<Long>>() {});
            return ids != null && ids.contains(courseId);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Progress service is not reachable");
        }
    }
}
