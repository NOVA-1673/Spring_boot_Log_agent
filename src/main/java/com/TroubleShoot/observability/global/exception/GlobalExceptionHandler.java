package com.troubleshoot.observability.global.exception;

import com.troubleshoot.observability.global.logging.TraceIdFilter;
import com.troubleshoot.observability.global.response.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(traceId, e.getMessage()));
    }
}
