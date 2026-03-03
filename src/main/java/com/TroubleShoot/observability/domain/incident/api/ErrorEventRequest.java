package com.troubleshoot.observability.domain.incident.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ErrorEventRequest(
        @NotBlank String serviceName,
        @NotNull Instant occurredAt,
        String traceId,
        String message,
        @NotBlank String exceptionClass,
        @NotBlank @Size(max = 65536) String stacktrace
) {
}
