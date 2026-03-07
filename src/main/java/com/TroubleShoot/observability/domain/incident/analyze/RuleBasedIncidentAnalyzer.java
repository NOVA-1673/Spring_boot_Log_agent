package com.troubleshoot.observability.domain.incident.analyze;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedIncidentAnalyzer implements IncidentAnalyzer {

    private static final String ANALYZER_VERSION = "rule-v1";
    private static final int MAX_EVIDENCE = 5;

    @Override
    public IncidentAnalysisResult analyze(Incident incident, List<IncidentEvent> events) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }

        List<IncidentEvent> safeEvents = events == null ? List.of() : events;
        String category = deriveCategory(incident, safeEvents);
        String severity = deriveSeverity(incident.getOccurrenceCount());
        String title = buildTitle(incident, category);
        String summary = buildSummary(incident);
        List<String> keyEvidence = buildKeyEvidence(incident, safeEvents);
        List<String> suspectedRootCauses = buildRootCauses(category);
        List<String> nextActions = buildNextActions(category);

        return new IncidentAnalysisResult(
                category,
                severity,
                title,
                summary,
                keyEvidence,
                suspectedRootCauses,
                nextActions,
                Instant.now(),
                ANALYZER_VERSION
        );
    }

    private String deriveCategory(Incident incident, List<IncidentEvent> events) {
        String exceptionClass = safeLower(incident.getExceptionClass());
        String sampleMessage = safeLower(incident.getSampleMessage());
        String eventText = safeLower(
                events.stream()
                        .map(IncidentEvent::getMessage)
                        .filter(this::hasText)
                        .reduce("", (a, b) -> a + " " + b)
        );

        String corpus = String.join(" ", exceptionClass, sampleMessage, eventText);

        boolean hasDbKeyword = containsAny(corpus, "sql", "sqlexception", "database");
        if (hasDbKeyword) {
            return "DB";
        }
        if (corpus.contains("timeout")) {
            return "NETWORK";
        }
        if (containsAny(corpus, "nullpointerexception", "illegalargumentexception")) {
            return "APP";
        }
        return "UNKNOWN";
    }

    private String deriveSeverity(int occurrenceCount) {
        if (occurrenceCount >= 100) {
            return "HIGH";
        }
        if (occurrenceCount >= 20) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildTitle(Incident incident, String category) {
        String serviceName = hasText(incident.getServiceName()) ? incident.getServiceName() : "unknown-service";
        String exceptionClass = hasText(incident.getExceptionClass()) ? incident.getExceptionClass() : category;
        return "[" + serviceName + "] " + exceptionClass + " incident";
    }

    private String buildSummary(Incident incident) {
        String serviceName = hasText(incident.getServiceName()) ? incident.getServiceName() : "unknown-service";
        String firstSeenAt = incident.getFirstSeenAt() == null ? "unknown" : incident.getFirstSeenAt().toString();
        String lastSeenAt = incident.getLastSeenAt() == null ? "unknown" : incident.getLastSeenAt().toString();
        return "Service " + serviceName + " recorded "
                + incident.getOccurrenceCount() + " occurrences between "
                + firstSeenAt + " and " + lastSeenAt + ".";
    }

    private List<String> buildKeyEvidence(Incident incident, List<IncidentEvent> events) {
        List<String> evidence = events.stream()
                .sorted(Comparator.comparing(IncidentEvent::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(IncidentEvent::getMessage)
                .filter(this::hasText)
                .limit(MAX_EVIDENCE)
                .toList();

        if (!evidence.isEmpty()) {
            return evidence;
        }
        if (hasText(incident.getSampleMessage())) {
            return List.of(incident.getSampleMessage().trim());
        }
        return List.of();
    }

    private List<String> buildRootCauses(String category) {
        return switch (category) {
            case "DB" -> List.of(
                    "Database connection pool exhaustion",
                    "Slow query or lock contention",
                    "Transient database connectivity instability"
            );
            case "NETWORK" -> List.of(
                    "Upstream dependency latency spike",
                    "Network path instability or packet loss"
            );
            case "APP" -> List.of(
                    "Missing null checks in application code",
                    "Invalid input passed to internal method",
                    "Unhandled edge-case in business logic"
            );
            default -> List.of("Insufficient signal for precise root-cause classification");
        };
    }

    private List<String> buildNextActions(String category) {
        return switch (category) {
            case "DB" -> List.of(
                    "Check DB health metrics and active connections",
                    "Inspect slow query and lock wait logs",
                    "Review recent schema or index changes"
            );
            case "NETWORK" -> List.of(
                    "Check upstream service latency and error rate",
                    "Verify timeout and retry configuration",
                    "Correlate with network infrastructure incidents"
            );
            case "APP" -> List.of(
                    "Inspect recent deploys around first seen time",
                    "Trace failing code path using stacktrace and traceId",
                    "Add input validation and null-safety guards",
                    "Create regression test for the observed scenario"
            );
            default -> List.of(
                    "Review incident timeline and correlated logs",
                    "Capture additional context from traces and metrics"
            );
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
