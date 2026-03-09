package com.troubleshoot.observability.domain.incident.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import com.troubleshoot.observability.domain.incident.analyze.IncidentAnalysisResult;
import com.troubleshoot.observability.domain.incident.analyze.IncidentAnalyzer;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentAnalysis;
import com.troubleshoot.observability.domain.incident.persistence.IncidentAnalysisRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentAnalysisService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final IncidentAnalyzer incidentAnalyzer;
    private final ObjectMapper objectMapper;

    public IncidentAnalysisService(
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            IncidentAnalysisRepository incidentAnalysisRepository,
            IncidentAnalyzer incidentAnalyzer,
            ObjectMapper objectMapper
    ) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.incidentAnalysisRepository = incidentAnalysisRepository;
        this.incidentAnalyzer = incidentAnalyzer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IncidentAnalysis analyzeIncident(Long incidentId, boolean force) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found. id=" + incidentId));

        Optional<IncidentAnalysis> existingAnalysis = incidentAnalysisRepository.findByIncident_Id(incidentId);
        if (existingAnalysis.isPresent() && !force) {
            return existingAnalysis.get();
        }

        assertAnalyzableStatus(incident);
        transitionIfNeeded(incident, IncidentStatus.ANALYZING);

        List<IncidentEvent> recentEvents = incidentEventRepository.findTop50ByIncidentIdOrderByOccurredAtDesc(incidentId);
        IncidentAnalysisResult result = incidentAnalyzer.analyze(incident, recentEvents);

        String keyEvidenceJson = toJson(result.keyEvidence());
        String suspectedRootCausesJson = toJson(result.suspectedRootCauses());
        String nextActionsJson = toJson(result.nextActions());

        IncidentAnalysis analysis = existingAnalysis
                .map(it -> {
                    it.updateFrom(
                            normalizeBlank(result.category()),
                            normalizeBlank(result.severity()),
                            normalizeBlank(result.title()),
                            normalizeBlank(result.summary()),
                            keyEvidenceJson,
                            suspectedRootCausesJson,
                            nextActionsJson,
                            result.analyzedAt(),
                            normalizeBlank(result.analyzerVersion())
                    );
                    return it;
                })
                .orElseGet(() -> new IncidentAnalysis(
                        incident,
                        normalizeBlank(result.category()),
                        normalizeBlank(result.severity()),
                        normalizeBlank(result.title()),
                        normalizeBlank(result.summary()),
                        keyEvidenceJson,
                        suspectedRootCausesJson,
                        nextActionsJson,
                        result.analyzedAt(),
                        normalizeBlank(result.analyzerVersion())
                ));

        IncidentAnalysis saved = incidentAnalysisRepository.save(analysis);
        transitionIfNeeded(incident, IncidentStatus.ANALYZED);
        return saved;
    }

    private void assertAnalyzableStatus(Incident incident) {
        IncidentStatus status = incident.getStatus();
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.IGNORED) {
            throw new IllegalStateException("Cannot analyze incident in status " + status);
        }
    }

    private void transitionIfNeeded(Incident incident, IncidentStatus nextStatus) {
        IncidentStatus current = incident.getStatus();
        if (current == nextStatus) {
            return;
        }
        if (nextStatus == IncidentStatus.ANALYZING
                && current != IncidentStatus.OPEN
                && current != IncidentStatus.ACKNOWLEDGED
                && current != IncidentStatus.ANALYZED) {
            return;
        }

        incident.transitionTo(nextStatus, nextStatus.name());
        incidentEventRepository.save(new IncidentEvent(
                incident,
                IncidentEventType.STATUS_CHANGED,
                nextStatus.name(),
                Instant.now(),
                incident.getPrimaryTraceId(),
                null
        ));
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNullElse(values, List.of()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize analysis list fields", e);
        }
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
