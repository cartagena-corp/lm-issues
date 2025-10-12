package com.cartagenacorp.lm_issues.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class ProjectExternalService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectExternalService.class);

    @Value("${project.service.url}")
    private String projectServiceUrl;

    private final RestTemplate restTemplate;

    public ProjectExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean validateProjectExists(UUID projectId, String token) {
        if (projectId == null) {
            return false;
        }
        logger.debug("[ProjectExternalService] [validateProjectExists] Validando la existencia del proyecto con ID={}", projectId);
        try {
            String url = projectServiceUrl + "/validate/" + projectId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            logger.info("[ProjectExternalService] [validateProjectExists] Resultado de la validación de existencia del proyecto con ID {}: {}", projectId, response);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("[ProjectExternalService] [validateProjectExists] Token no autorizado para validar la existencia del proyecto: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("[ProjectExternalService] [validateProjectExists] No tiene permisos para  validar la existencia del proyecto: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("[ProjectExternalService] [validateProjectExists] El servicio externo no esta disponible: {}",ex.getMessage());
        }  catch (Exception ex) {
            logger.error("[ProjectExternalService] [validateProjectExists] Error al validar la existencia del proyecto: {}", ex.getMessage(), ex);
        }
        return false;
    }

    public boolean validateProjectParticipant(UUID projectId, String token) {
        logger.debug("[ProjectExternalService] [validateProjectParticipant] Validando participación en el proyecto con ID={}", projectId);
        try {
            String url = projectServiceUrl + "/validateParticipant/" + projectId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Boolean.class
            );

            logger.info("[ProjectExternalService] [validateProjectParticipant] Resultado validación de participación en proyecto {}: {}", projectId, response);
            return Boolean.TRUE.equals(response.getBody());
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("[ProjectExternalService] [validateProjectParticipant] Proyecto no encontrado: {}", ex.getMessage());
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("[ProjectExternalService] [validateProjectParticipant] Token no autorizado para validar participación en proyecto: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("[ProjectExternalService] [validateProjectParticipant] No tiene permisos para validar participación en proyecto: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("[ProjectExternalService] [validateProjectParticipant] El servicio externo no está disponible: {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("[ProjectExternalService] [validateProjectParticipant] Error al validar participación en proyecto: {}", ex.getMessage(), ex);
        }
        return false;
    }
}
