package com.troubleshoot.observability.domain.incident.analyze;

import java.time.Instant;
import java.util.List;

public record IncidentAnalysisResult(
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
    public IncidentAnalysisResult {
        keyEvidence = keyEvidence == null ? List.of() : List.copyOf(keyEvidence);
        suspectedRootCauses = suspectedRootCauses == null ? List.of() : List.copyOf(suspectedRootCauses);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
