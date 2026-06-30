package com.tonepilot.starter.security;

import com.tonepilot.application.agent.*;
import com.tonepilot.application.agent.workflow.*;
import com.tonepilot.application.agent.workflow.node.*;
import com.tonepilot.application.controller.*;
import com.tonepilot.application.controller.admin.*;
import com.tonepilot.application.dto.*;
import com.tonepilot.application.evaluation.*;
import com.tonepilot.application.knowledge.*;
import com.tonepilot.application.photo.*;
import com.tonepilot.application.runtime.*;
import com.tonepilot.application.style.*;
import com.tonepilot.domain.agent.*;
import com.tonepilot.domain.agent.workflow.*;
import com.tonepilot.domain.colorgrading.*;
import com.tonepilot.domain.common.*;
import com.tonepilot.domain.evaluation.*;
import com.tonepilot.domain.knowledge.*;
import com.tonepilot.domain.observability.*;
import com.tonepilot.domain.photo.*;
import com.tonepilot.domain.runtime.*;
import com.tonepilot.domain.storage.*;
import com.tonepilot.domain.style.*;
import com.tonepilot.repository.observability.*;
import com.tonepilot.repository.runtime.*;
import com.tonepilot.infrastructure.agent.*;
import com.tonepilot.infrastructure.ai.*;
import com.tonepilot.infrastructure.ai.dto.*;
import com.tonepilot.infrastructure.knowledge.douyin.*;
import com.tonepilot.infrastructure.knowledge.rag.*;
import com.tonepilot.infrastructure.knowledge.rag.config.*;
import com.tonepilot.infrastructure.observability.*;
import com.tonepilot.infrastructure.observability.config.*;
import com.tonepilot.infrastructure.observability.repository.*;
import com.tonepilot.infrastructure.runtime.repository.*;
import com.tonepilot.infrastructure.shared.persistence.*;
import com.tonepilot.infrastructure.storage.*;
import com.tonepilot.infrastructure.storage.config.*;
import com.tonepilot.starter.advice.*;
import com.tonepilot.starter.bootstrap.*;
import com.tonepilot.starter.config.*;
import com.tonepilot.starter.security.*;







import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tonepilot.application.dto.ApiResponse;
import com.tonepilot.infrastructure.observability.ObservabilityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiGuardFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-TonePilot-Api-Key";

    private final SecurityProperties securityProperties;
    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final ObservabilityService observabilityService;
    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    @Autowired
    public ApiGuardFilter(
            SecurityProperties securityProperties,
            RateLimitProperties rateLimitProperties,
            ObjectMapper objectMapper,
            ObservabilityService observabilityService
    ) {
        this.securityProperties = securityProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
        this.observabilityService = observabilityService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!apiKeyAllowed(request)) {
            observabilityService.recordAuditEvent(
                    "security.api_key_rejected",
                    clientKey(request),
                    null,
                    "http",
                    request.getRequestURI(),
                    "API Key 校验失败"
            );
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "API Key 校验失败");
            return;
        }

        if (!rateLimitAllowed(request)) {
            observabilityService.recordAuditEvent(
                    "security.rate_limited",
                    clientKey(request),
                    null,
                    "http",
                    request.getRequestURI(),
                    "请求频率超过限制"
            );
            writeError(response, 429, "请求过于频繁，请稍后重试");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean apiKeyAllowed(HttpServletRequest request) {
        if (!securityProperties.isApiKeyEnabled()) {
            return true;
        }
        String configured = securityProperties.getApiKey();
        if (configured == null || configured.isBlank()) {
            return false;
        }
        return configured.equals(request.getHeader(API_KEY_HEADER));
    }

    private boolean rateLimitAllowed(HttpServletRequest request) {
        if (!rateLimitProperties.isEnabled()) {
            return true;
        }
        int limit = Math.max(1, rateLimitProperties.getRequestsPerMinute());
        long currentMinute = Instant.now().getEpochSecond() / 60;
        String key = clientKey(request);
        RateWindow window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new RateWindow(currentMinute);
            }
            return existing;
        });
        return window.count.incrementAndGet() <= limit;
    }

    private String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }

    private static class RateWindow {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger();

        private RateWindow(long minute) {
            this.minute = minute;
        }
    }
}


