package com.cartagenacorp.lm_issues.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
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

    public UUID authenticateUser(String email) {
        try {
            String loginUrl = authServiceUrl + "/login";
            Map<String, String> request = new HashMap<>();
            request.put("email", email);

            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return UUID.fromString(response.getBody().get("id").toString());
            } else {
                throw new RuntimeException("Authentication error");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to the authentication service: " + e.getMessage());
        }
    }

    public boolean userExists(UUID userId) {
        try {
            String url = authServiceUrl + "/auth/" + userId;
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}

