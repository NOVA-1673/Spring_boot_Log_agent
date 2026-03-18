package com.troubleshoot.observability.domain.incident.api;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        Long incidentId,
        String status,
        String category,
        String severity,
        String title,
        String summary,
        List<String> keyEvidence,
        List<String> suspectedRootCauses,
        List<String> nextActions,
        Instant analyzedAt,
        String analyzerVersion
) {
}
