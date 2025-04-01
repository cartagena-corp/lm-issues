package com.cartagenacorp.lm_issues.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class UserValidationService {

    @Value("${auth.service.url}")
    private String authServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public UserValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean userExists(UUID userId, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    authServiceUrl + "/validate/" + userId,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            System.out.println("Error validating user: " + e.getMessage());
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    authServiceUrl + "/token",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getBody() != null) {
                return UUID.fromString(response.getBody());
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
