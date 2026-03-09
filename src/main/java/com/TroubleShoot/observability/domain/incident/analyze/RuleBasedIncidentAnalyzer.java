package com.troubleshoot.observability.domain.incident.analyze;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
                        .filter(Objects::nonNull)
                        .map(IncidentEvent::getMessage)
                        .filter(this::hasText)
                        .reduce((a, b) -> a + " " + b)
                        .orElse("")
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
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(IncidentEvent::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(IncidentEvent::getMessage)
                .filter(this::hasText)
                .map(String::trim)
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
                    "Database latency",
                    "Connection pool exhaustion",
                    "Query slowdown"
            );
            case "NETWORK" -> List.of(
                    "Upstream timeout",
                    "Network instability",
                    "Downstream dependency slowness"
            );
            case "APP" -> List.of(
                    "Application bug",
                    "Invalid input handling",
                    "Null/state handling issue"
            );
            default -> List.of("Requires manual investigation");
        };
    }

    private List<String> buildNextActions(String category) {
        return switch (category) {
            case "DB" -> List.of(
                    "Check DB latency",
                    "Inspect connection pool",
                    "Review slow queries"
            );
            case "NETWORK" -> List.of(
                    "Inspect downstream response times",
                    "Check timeout settings",
                    "Verify network path"
            );
            case "APP" -> List.of(
                    "Inspect stack trace",
                    "Reproduce request flow",
                    "Check recent code changes"
            );
            default -> List.of(
                    "Inspect logs",
                    "Review recent deploys",
                    "Gather more evidence"
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
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
