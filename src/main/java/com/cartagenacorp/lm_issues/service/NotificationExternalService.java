package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.NotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class NotificationExternalService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationExternalService.class);

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public NotificationExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(UUID userId, String message, String type, Map<String, Object> metadata, UUID projectId, UUID issueId) {
        if (userId == null || message == null || type == null) {
            throw new IllegalArgumentException("Faltan campos de notificación obligatorios");
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
            logger.info("[NotificationExternalService] [sendNotification] Realizando envio de notificación: {}", request);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("[NotificationExternalService] [sendNotification] Token no autorizado: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("[NotificationExternalService] [sendNotification] Permisos insuficientes: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("[NotificationExternalService] [sendNotification] El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("[NotificationExternalService] [sendNotification] Error al enviar la notificación: {}", ex.getMessage(), ex);
        }
    }
}

