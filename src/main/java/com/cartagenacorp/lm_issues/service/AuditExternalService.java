package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.IssueHistoryDto;
import com.cartagenacorp.lm_issues.entity.Issue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class AuditExternalService {

    private static final Logger logger = LoggerFactory.getLogger(AuditExternalService.class);

    @Value("${audit.service.url}")
    private String auditServiceUrl;

    private final RestTemplate restTemplate;

    public AuditExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void logChange(UUID issueId, String issueTitle, UUID userId, String action, String description, UUID projectId, Issue beforeChange, Issue afterChange, String token) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String beforeJson = beforeChange != null ? mapper.writeValueAsString(beforeChange) : null;
        String afterJson  = afterChange  != null ? mapper.writeValueAsString(afterChange)  : null;

        IssueHistoryDto auditLog = IssueHistoryDto.builder()
                .issueId(issueId)
                .issueTitle(issueTitle)
                .userId(userId)
                .action(action)
                .description(description)
                .projectId(projectId)
                .beforeChange(beforeJson)
                .afterChange(afterJson)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<IssueHistoryDto> request = new HttpEntity<>(auditLog, headers);

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
