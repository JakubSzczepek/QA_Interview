package com.recruitment.skybook.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet filter that captures request/response payloads and stores them in ApiLogStore.
 * Only logs /api/** requests (skips static files, swagger, logs page).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RequestResponseLoggingFilter implements Filter {

    private final ApiLogStore logStore;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI();

        // Only log API calls (skip static, swagger, actuator, logs and recruiter endpoints)
        if (!uri.startsWith("/api/") || uri.startsWith("/api/v1/logs") || uri.startsWith("/api/v1/recruiter")) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        boolean thrown = false;

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception ex) {
            thrown = true;
            long duration = System.currentTimeMillis() - start;
            String requestBody = getBody(wrappedRequest.getContentAsByteArray());
            logStore.add(request.getMethod(), uri, request.getQueryString(),
                    500, requestBody, "{\"error\":\"" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\"}",
                    duration);
            wrappedResponse.copyBodyToResponse();
            throw ex;
        } finally {
            if (!thrown) {
                long duration = System.currentTimeMillis() - start;
                String requestBody = getBody(wrappedRequest.getContentAsByteArray());
                String responseBody = getBody(wrappedResponse.getContentAsByteArray());
                logStore.add(request.getMethod(), uri, request.getQueryString(),
                        wrappedResponse.getStatus(), requestBody, responseBody, duration);
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    private String getBody(byte[] content) {
        if (content == null || content.length == 0) return null;
        return new String(content, StandardCharsets.UTF_8);
    }
}
