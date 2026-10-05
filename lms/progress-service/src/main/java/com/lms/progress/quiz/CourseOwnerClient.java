package com.lms.progress.quiz;

import com.lms.progress.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Progress does not know who owns a course. So it asks the Course service,
 * passing the instructor's own token: Course answers 403 if it is not theirs.
 */
@Component
public class CourseOwnerClient {

    private final RestClient client;

    public CourseOwnerClient(@Value("${app.course-service-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void requireOwner(Long courseId, String authorizationHeader) {
        try {
            client.get().uri("/api/courses/{id}", courseId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Course not found");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This is not your course");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Course service is not reachable");
        }
    }
}
