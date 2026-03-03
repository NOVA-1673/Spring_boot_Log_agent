package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.IncidentStatus;
import java.time.Instant;
import java.util.List;

public record IncidentDetailResponse(
        Long id,
        IncidentStatus status,
        String serviceName,
        String exceptionClass,
        int occurrenceCount,
        Instant firstSeenAt,
        Instant lastSeenAt,
        List<IncidentEventSummary> events
) {
}
