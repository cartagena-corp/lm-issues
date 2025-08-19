package com.cartagenacorp.lm_issues.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class ProjectValidationService {

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public ProjectValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateProjectExists(UUID projectId) {
        if (projectId == null) {
            return false;
        }
        try {
            String url = projectServiceUrl + "/validate/" + projectId;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (Exception ex) {
            System.out.println("Error validating project: " + ex.getMessage());
            return false;
        }
    }
}
