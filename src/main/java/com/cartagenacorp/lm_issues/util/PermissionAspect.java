package com.cartagenacorp.lm_issues.util;

import com.cartagenacorp.lm_issues.exceptions.BaseException;
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
            throw new BaseException("Authorization header faltante o no válido", HttpStatus.UNAUTHORIZED.value());
        }

        String token = authHeader.substring(7);

        if (!jwtTokenUtil.validateToken(token)) {
            throw new BaseException("Token inválido o caducado", HttpStatus.UNAUTHORIZED.value());
        }

        List<String> permissions = jwtTokenUtil.getPermissionsFromToken(token);

        UUID userId = jwtTokenUtil.getUserId(token);
        UUID organizationId = jwtTokenUtil.getOrganizationId(token);
        JwtContextHolder.setUserId(userId);
        JwtContextHolder.setToken(token);
        JwtContextHolder.setOrganizationId(organizationId);

        try {
            boolean hasPermission = Arrays.stream(requiresPermission.value())
                    .anyMatch(permissions::contains);

            if (!hasPermission) {
                throw new BaseException("Permisos insuficientes", HttpStatus.FORBIDDEN.value());
            }

            return joinPoint.proceed();
        } finally {
            JwtContextHolder.clear();
        }


    }
}

