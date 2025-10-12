package com.cartagenacorp.lm_issues.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditExternalService {

    private static final Logger logger = LoggerFactory.getLogger(AuditExternalService.class);

    @Value("${audit.service.url}")
    private String auditServiceUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public AuditExternalService(RestTemplate restTemplate) {
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

        try {
            logger.info("[AuditExternalService] [logChange] Realizando envio de auditoria: {}", auditLog);
            restTemplate.postForEntity(auditServiceUrl + "/logChange", request, Void.class);
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("[AuditExternalService] [logChange] Token no autorizado: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("[AuditExternalService] [logChange] Permisos insuficientes: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("[AuditExternalService] [logChange] El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("[AuditExternalService] [logChange] Error al enviar la auditoria: {}", ex.getMessage(), ex);
        }

    }
}
