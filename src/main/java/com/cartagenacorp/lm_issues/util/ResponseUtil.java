package com.cartagenacorp.lm_issues.util;

import com.cartagenacorp.lm_issues.dto.NotificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class ResponseUtil {

    public static NotificationResponse success(String message) {
        return success(message, HttpStatus.OK);
    }

    public static NotificationResponse success(String message, HttpStatus status) {
        return NotificationResponse.success(
                message,
                status.value(),
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString()
        );
    }

    public static NotificationResponse error(String message, HttpStatus status) {
        return NotificationResponse.error(
                message,
                status.value(),
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString()
        );
    }
}
