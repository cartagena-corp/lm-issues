package com.cartagenacorp.lm_issues.service;

import com.cartagenacorp.lm_issues.dto.SprintDto;
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
public class SprintExternalService {

    private static final Logger logger = LoggerFactory.getLogger(SprintExternalService.class);

    @Value("${sprint.service.url}")
    private String sprintServiceUrl;

    private final RestTemplate restTemplate;

    public SprintExternalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SprintDto getSprintById(UUID sprintId, String token) {
        try {
            String url = sprintServiceUrl + "/" + sprintId;
            logger.info("[SprintExternalService] [getSprintById] Llamando al servicio externo para obtener el Sprint con ID={}", sprintId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<SprintDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    SprintDto.class
            );
            logger.info("[SprintExternalService] [getSprintById] Sprint con ID={} obtenido exitosamente", sprintId);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            logger.warn("[SprintExternalService] [getSprintById] Sprint no encontrado: {}", sprintId);
        } catch (HttpClientErrorException.Unauthorized ex) {
            logger.warn("[SprintExternalService] [getSprintById] Token no autorizado: {}", ex.getMessage());
        } catch (HttpClientErrorException.Forbidden ex) {
            logger.warn("[SprintExternalService] [getSprintById] Permisos insuficientes: {}", ex.getMessage());
        } catch (ResourceAccessException ex) {
            logger.warn("[SprintExternalService] [getSprintById] El servicio externo no esta disponible: {}",ex.getMessage());
        } catch (Exception ex) {
            logger.error("[SprintExternalService] [getSprintById] Error al obtener el Sprint con ID={}: {}", sprintId, ex.getMessage(), ex);
        }
        return null;
    }
}
