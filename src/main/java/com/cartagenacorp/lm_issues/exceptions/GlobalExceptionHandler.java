package com.cartagenacorp.lm_issues.exceptions;

import com.cartagenacorp.lm_issues.dto.NotificationResponse;
import com.cartagenacorp.lm_issues.util.ConstantUtil;
import com.cartagenacorp.lm_issues.util.ResponseUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<NotificationResponse> handleBaseException(BaseException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ResponseUtil.error(ex.getMessage(), HttpStatus.valueOf(ex.getStatusCode())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<NotificationResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("uuid")
                ? ConstantUtil.INVALID_UUID
                : ConstantUtil.INVALID_INPUT;

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtil.error(message, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<NotificationResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<NotificationResponse> handleFileStorageException(FileStorageException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NotificationResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String combinedErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        return ResponseEntity.badRequest()
                .body(ResponseUtil.error(combinedErrors, HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<NotificationResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.error(ConstantUtil.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<NotificationResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseUtil.error(ConstantUtil.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<NotificationResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseUtil.error(ConstantUtil.DATA_INTEGRITY_FAIL_MESSAGE, HttpStatus.CONFLICT));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<NotificationResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseUtil.error(ConstantUtil.DATA_INTEGRITY_FAIL_MESSAGE, HttpStatus.CONFLICT));
    }
}

