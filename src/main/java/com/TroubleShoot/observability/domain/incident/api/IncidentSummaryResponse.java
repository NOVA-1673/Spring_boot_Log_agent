package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.IncidentStatus;
import java.time.Instant;

public record IncidentSummaryResponse(
        Long id,
        IncidentStatus status,
        String serviceName,
        String exceptionClass,
        int occurrenceCount,
        Instant lastSeenAt
) {
}
