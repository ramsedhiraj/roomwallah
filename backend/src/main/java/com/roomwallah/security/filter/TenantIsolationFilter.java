package com.roomwallah.security.filter;

import com.roomwallah.security.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantIsolationFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            String tenantId = httpRequest.getHeader(TENANT_HEADER);
            if (tenantId == null || tenantId.trim().isEmpty()) {
                tenantId = httpRequest.getParameter("tenantId");
            }
            
            // Set the tenant context
            TenantContext.setCurrentTenant(tenantId);
            log.debug("TenantContext initialized: {} for URI: {}", TenantContext.getCurrentTenant(), httpRequest.getRequestURI());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Clear context to prevent thread-local leakage
            TenantContext.clear();
        }
    }
}
