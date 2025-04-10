package com.cartagenacorp.lm_issues.util;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtTokenUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        List<String> permissions = jwtTokenUtil.getPermissionsFromToken(token);

        UUID userId = jwtTokenUtil.getUserId(token);
        JwtContextHolder.setUserId(userId);
        JwtContextHolder.setToken(token);

        try {
            boolean hasPermission = Arrays.stream(requiresPermission.value())
                    .anyMatch(permissions::contains);

            if (!hasPermission) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
            }

            return joinPoint.proceed();
        } finally {
            JwtContextHolder.clear();
        }


    }
}

