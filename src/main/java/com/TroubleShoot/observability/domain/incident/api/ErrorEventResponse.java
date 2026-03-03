package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.IncidentStatus;

public record ErrorEventResponse(
        Long incidentId,
        IncidentStatus status,
        boolean grouped
) {
}
