package com.cartagenacorp.lm_issues.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    @Value("${audit.service.url}")
    private String auditServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public AuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void logChange(UUID issueId, UUID userId, String action, String description, UUID projectId) {
        Map<String, Object> auditLog = new HashMap<>();
        auditLog.put("issueId", issueId);
        auditLog.put("userId", userId);
        auditLog.put("action", action);
        auditLog.put("description", description);
        auditLog.put("projectId", projectId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(auditLog, headers);

        restTemplate.postForEntity(auditServiceUrl + "/logChange", request, Void.class);
    }
}
