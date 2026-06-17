package com.roomwallah.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomwallah.audit.Audit;
import com.roomwallah.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        String operator = getOperator();
        String action = audit.action();
        String targetEntity = audit.targetEntity();
        String payload = getPayload(joinPoint.getArgs());
        String targetEntityId = null;

        if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] != null) {
            targetEntityId = joinPoint.getArgs()[0].toString();
        }

        Object result;
        try {
            result = joinPoint.proceed();
            auditLogService.logAsync(action, operator, targetEntity, targetEntityId, "SUCCESS", payload, null);
            return result;
        } catch (Throwable throwable) {
            auditLogService.logAsync(action, operator, targetEntity, targetEntityId, "FAILURE", payload, throwable.getMessage());
            throw throwable;
        }
    }

    private String getOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String clientIp = request.getRemoteAddr();
            return "ANONYMOUS_IP_" + clientIp;
        }
        return "SYSTEM";
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getPayload(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }
}
