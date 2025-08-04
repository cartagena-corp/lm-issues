package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.UserBasicDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
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

    public List<UUID> getParticipantIds(UUID projectId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserBasicDataDto[]> response = restTemplate.exchange(
                projectServiceUrl + "/" + projectId + "/participants",
                HttpMethod.GET,
                entity,
                UserBasicDataDto[].class
        );

        return Arrays.stream(response.getBody())
                .map(UserBasicDataDto::getId)
                .toList();
    }
}
