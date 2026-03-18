package com.troubleshoot.observability.domain.incident.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.troubleshoot.observability.domain.incident.persistence.IncidentAnalysis;
import com.troubleshoot.observability.domain.incident.persistence.IncidentAnalysisRepository;
import com.troubleshoot.observability.domain.incident.service.IncidentAnalysisService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/incidents")
public class IncidentAnalysisController {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final IncidentAnalysisService incidentAnalysisService;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final ObjectMapper objectMapper;

    public IncidentAnalysisController(
            IncidentAnalysisService incidentAnalysisService,
            IncidentAnalysisRepository incidentAnalysisRepository,
            ObjectMapper objectMapper
    ) {
        this.incidentAnalysisService = incidentAnalysisService;
        this.incidentAnalysisRepository = incidentAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{id}/analyze")
    public AnalysisResponse analyzeIncident(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        try {
            return toResponse(incidentAnalysisService.analyzeIncident(id, force));
        } catch (EntityNotFoundException | NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found: " + id, ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}/analysis")
    public AnalysisResponse getIncidentAnalysis(@PathVariable Long id) {
        IncidentAnalysis analysis = incidentAnalysisRepository.findByIncident_Id(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found for incident: " + id));
        return toResponse(analysis);
    }

    private AnalysisResponse toResponse(IncidentAnalysis analysis) {
        return new AnalysisResponse(
                analysis.getIncident().getId(),
                analysis.getIncident().getStatus() != null ? analysis.getIncident().getStatus().name() : null,
                analysis.getCategory(),
                analysis.getSeverity(),
                analysis.getTitle(),
                analysis.getSummary(),
                readStringList(analysis.getKeyEvidenceJson()),
                readStringList(analysis.getSuspectedRootCausesJson()),
                readStringList(analysis.getNextActionsJson()),
                analysis.getAnalyzedAt(),
                analysis.getAnalyzerVersion()
        );
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read analysis payload", ex);
        }
    }
}
