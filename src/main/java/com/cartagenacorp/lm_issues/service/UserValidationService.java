package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.UserBasicDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
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

    public Optional<UserBasicDataDto> getUserData(String token, String userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<UserBasicDataDto> response = restTemplate.exchange(
                    authServiceUrl + "/user/" + userId,
                    HttpMethod.GET,
                    entity,
                    UserBasicDataDto.class
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            System.out.println("Error getting user: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<List<UserBasicDataDto>> getUsersData(String token, List<String> ids) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<String>> entity = new HttpEntity<>(ids, headers);

            ResponseEntity<List<UserBasicDataDto>> response = restTemplate.exchange(
                    authServiceUrl + "/users/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            System.out.println("Error getting users: " + e.getMessage());
            return Optional.empty();
        }
    }
}
