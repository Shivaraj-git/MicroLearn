package com.lms.enrollment;

import com.lms.enrollment.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Asks the Course service for the real price. We never trust a price sent by the client. */
@Component
public class CatalogClient {

    private final RestClient client;

    public CatalogClient(@Value("${app.course-service-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public CatalogItem find(Long courseId) {
        try {
            CatalogItem item = client.get().uri("/api/catalog/{id}", courseId)
                    .retrieve().body(CatalogItem.class);
            if (item == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Course not found or not published yet");
            }
            return item;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Course not found or not published yet");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Course service is not reachable");
        }
    }
}
