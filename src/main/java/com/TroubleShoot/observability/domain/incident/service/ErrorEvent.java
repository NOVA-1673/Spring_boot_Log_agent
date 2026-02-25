package com.troubleshoot.observability.domain.incident.service;

import java.time.Instant;
import java.util.Objects;

public final class ErrorEvent {

    private final String serviceName;
    private final Instant occurredAt;
    private final String traceId;
    private final String message;
    private final String exceptionClass;
    private final String stacktrace;

    public ErrorEvent(String serviceName,
                      Instant occurredAt,
                      String traceId,
                      String message,
                      String exceptionClass,
                      String stacktrace) {
        this.serviceName = Objects.requireNonNull(serviceName, "serviceName");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.exceptionClass = Objects.requireNonNull(exceptionClass, "exceptionClass");
        this.stacktrace = Objects.requireNonNull(stacktrace, "stacktrace");
        this.traceId = traceId;
        this.message = message;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getMessage() {
        return message;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getStacktrace() {
        return stacktrace;
    }
}
