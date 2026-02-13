package com.troubleshoot.observability.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter  extends OncePerRequestFilter{
    public static final String TRACE_ID = "traceId";
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 기존 헤더가 있으면 그걸 쓰고, 없으면 생성
        String incoming = request.getHeader(TRACE_HEADER);
        String traceId = (incoming != null && !incoming.isBlank())
                ? incoming
                : UUID.randomUUID().toString().substring(0, 8);

        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // ✅ thread 재사용 때문에 꼭 clear
        }
    }
}
