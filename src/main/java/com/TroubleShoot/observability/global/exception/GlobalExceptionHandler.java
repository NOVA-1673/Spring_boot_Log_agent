package com.troubleshoot.observability.global.exception;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.service.IncidentWriter;
import com.troubleshoot.observability.global.logging.TraceIdFilter;
import com.troubleshoot.observability.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final IncidentWriter incidentWriter;

    public GlobalExceptionHandler(IncidentWriter incidentWriter) {
        this.incidentWriter = incidentWriter;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e, HttpServletRequest req) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);

        String category = categorize(e);
        String severity = "HIGH";

        Incident incident = new Incident(
                traceId == null ? "UNKNOWN" : traceId,
                category,
                severity,
                500,
                req.getMethod(),
                req.getRequestURI(),
                safeMsg(e)
        );
        try {
            incidentWriter.write(incident);
        } catch (Exception ignored) {
            // 예외 처리 중 저장 실패가 응답까지 깨지지 않게
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(traceId, e.getMessage()));
    }

    private String categorize(Exception e) {
        String name = e.getClass().getSimpleName();
        if (name.contains("NullPointer")) return "BUG_NULL";
        if (name.contains("AccessDenied")) return "AUTHZ";
        if (name.contains("MethodArgumentNotValid")) return "VALIDATION";
        return "UNKNOWN";
    }

    private String safeMsg(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.isBlank()) ? e.getClass().getName() : msg;
    }
}
