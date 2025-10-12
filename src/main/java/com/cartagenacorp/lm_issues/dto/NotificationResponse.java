package com.cartagenacorp.lm_issues.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private String message;
    private String date;
    private String time;
    private Integer statusCode;
    private String path;

    public static NotificationResponse success(String message, int statusCode, String path) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationResponse.builder()
                .message(message)
                .date(now.toLocalDate().toString())
                .time(now.toLocalTime().toString())
                .statusCode(statusCode)
                .path(path)
                .build();
    }

    public static NotificationResponse error(String message, int statusCode, String path) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationResponse.builder()
                .message(message)
                .date(now.toLocalDate().toString())
                .time(now.toLocalTime().toString())
                .statusCode(statusCode)
                .path(path)
                .build();
    }
}
