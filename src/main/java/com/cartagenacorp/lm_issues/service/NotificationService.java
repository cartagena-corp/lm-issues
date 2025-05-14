package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.NotificationDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(UUID userId, String message, String type, Map<String, Object> metadata, UUID projectId, UUID issueId) {
        if (userId == null || message == null || type == null) {
            throw new IllegalArgumentException("Missing required notification fields");
        }

        String url = notificationServiceUrl + "/send";

        NotificationDTO request = new NotificationDTO();
        request.setUserId(userId);
        request.setMessage(message);
        request.setType(type);
        request.setMetadata(metadata);
        request.setProjectId(projectId);
        request.setIssueId(issueId);

        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new IllegalArgumentException("Invalid notification request: " + e.getResponseBodyAsString());
        } catch (HttpClientErrorException.NotFound e) {
            throw new EntityNotFoundException("User not found for notification");
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification: " + e.getMessage());
        }
    }
}

