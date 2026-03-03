package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.persistence.IncidentEventType;
import java.time.Instant;

public record IncidentEventSummary(
        Long id,
        IncidentEventType type,
        Instant occurredAt,
        String traceId,
        String message,
        String note
) {
}
