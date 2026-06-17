package com.roomwallah.partner;

import com.roomwallah.partner.domain.PartnerApiKey;
import com.roomwallah.partner.filter.ApiKeyAuthenticationFilter;
import com.roomwallah.partner.service.ApiKeyValidationResult;
import com.roomwallah.partner.service.PartnerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ApiKeyAuthenticationFilterTest {

    @Mock
    private PartnerService partnerService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new ApiKeyAuthenticationFilter(partnerService);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testDoFilterInternal_MissingHeader_PassesThrough() throws Exception {
        when(request.getHeader("X-Partner-Key")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(partnerService, never()).validateApiKey(anyString());
    }

    @Test
    public void testDoFilterInternal_ValidKey_Authenticates() throws Exception {
        String rawKey = "dummy-raw-key";
        when(request.getHeader("X-Partner-Key")).thenReturn(rawKey);
        when(partnerService.validateApiKey(rawKey)).thenReturn(ApiKeyValidationResult.VALID);

        PartnerApiKey keyEntity = PartnerApiKey.builder()
                .partnerName("GooglePartner")
                .scopes("read-only,booking")
                .build();
        when(partnerService.getApiKeyByHashCached(anyString())).thenReturn(keyEntity);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("GooglePartner", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    public void testDoFilterInternal_QuotaExceeded_Returns429() throws Exception {
        String rawKey = "dummy-raw-key";
        when(request.getHeader("X-Partner-Key")).thenReturn(rawKey);
        when(partnerService.validateApiKey(rawKey)).thenReturn(ApiKeyValidationResult.QUOTA_EXCEEDED);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, filterChain);

        verify(response, times(1)).setStatus(429);
        verify(filterChain, never()).doFilter(request, response);
    }
}
