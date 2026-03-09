package com.troubleshoot.observability.domain.incident.persistence;

import com.troubleshoot.observability.domain.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "incident_analysis",
        indexes = {
                @Index(name = "idx_incident_analysis_incident", columnList = "incident_id")
        }
)
public class IncidentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "severity", length = 16)
    private String severity;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "key_evidence", columnDefinition = "text")
    private String keyEvidenceJson;

    @Column(name = "suspected_root_causes", columnDefinition = "text")
    private String suspectedRootCausesJson;

    @Column(name = "next_actions", columnDefinition = "text")
    private String nextActionsJson;

    @Column(name = "analyzer_version", nullable = false, length = 32)
    private String analyzerVersion;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IncidentAnalysis() {
    }

    public IncidentAnalysis(
            Incident incident,
            String category,
            String severity,
            String title,
            String summary,
            String keyEvidenceJson,
            String suspectedRootCausesJson,
            String nextActionsJson,
            Instant analyzedAt,
            String analyzerVersion
    ) {
        this.incident = incident;
        this.category = category;
        this.severity = severity;
        this.title = title;
        this.summary = summary;
        this.keyEvidenceJson = keyEvidenceJson;
        this.suspectedRootCausesJson = suspectedRootCausesJson;
        this.nextActionsJson = nextActionsJson;
        this.analyzedAt = analyzedAt;
        this.analyzerVersion = analyzerVersion;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.analyzerVersion == null || this.analyzerVersion.isBlank()) {
            this.analyzerVersion = "rule-v1";
        }
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getKeyEvidenceJson() {
        return keyEvidenceJson;
    }

    public String getSuspectedRootCausesJson() {
        return suspectedRootCausesJson;
    }

    public String getNextActionsJson() {
        return nextActionsJson;
    }

    public String getAnalyzerVersion() {
        return analyzerVersion;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() { return updatedAt; }

    public void updateFrom(
            String category,
            String severity,
            String title,
            String summary,
            String keyEvidenceJson,
            String suspectedRootCausesJson,
            String nextActionsJson,
            Instant analyzedAt,
            String analyzerVersion
    ) {
        this.category = category;
        this.severity = severity;
        this.title = title;
        this.summary = summary;
        this.keyEvidenceJson = keyEvidenceJson;
        this.suspectedRootCausesJson = suspectedRootCausesJson;
        this.nextActionsJson = nextActionsJson;
        this.analyzedAt = analyzedAt;
        this.analyzerVersion = (analyzerVersion == null || analyzerVersion.isBlank()) ? "rule-v1" : analyzerVersion;
        this.updatedAt = Instant.now();
    }
}
