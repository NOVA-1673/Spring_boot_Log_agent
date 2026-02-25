package com.troubleshoot.observability.domain.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident")
public class Incident {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 64)
    private String traceId;

    @Column(nullable = true, length = 32)
    private String category; // 예: DB, AUTH, VALIDATION, NPE ...

    @Column(nullable = true, length = 16)
    private String severity; // 예: LOW/MEDIUM/HIGH

    @Column(nullable = true, length = 16)
    private int statusCode;

    @Column(nullable = true, length = 16)
    private String method;

    @Column(nullable = true, length = 255)
    private String path;

    @Column(nullable = true, length = 2000)
    private String message;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "signature_hash", length = 64)
    private String signatureHash;

    @Column(name = "exception_class", length = 255)
    private String exceptionClass;

    @Column(name = "first_seen_at")
    private Instant firstSeenAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "occurrence_count")
    private int occurrenceCount;

    @Column(name = "primary_trace_id", length = 64)
    private String primaryTraceId;

    @Column(name = "sample_message", length = 2000)
    private String sampleMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "text")
    private String resolutionNote;

    // 동시성 보호(상태 변경 충돌 방지)
    @Version
    private long version;

    protected Incident() {}

    public Incident(String traceId, String category, String severity, int statusCode,
                    String method, String path, String message) {
        this.traceId = traceId;
        this.category = category;
        this.severity = severity;
        this.statusCode = statusCode;
        this.method = method;
        this.path = path;
        this.message = message;
        this.status = IncidentStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Incident(String serviceName,
                    String signatureHash,
                    String exceptionClass,
                    Instant occurredAt,
                    String primaryTraceId,
                    String sampleMessage) {
        this.serviceName = serviceName;
        this.signatureHash = signatureHash;
        this.exceptionClass = exceptionClass;
        this.firstSeenAt = occurredAt;
        this.lastSeenAt = occurredAt;
        this.occurrenceCount = 1;
        this.primaryTraceId = primaryTraceId;
        this.sampleMessage = sampleMessage;
        this.status = IncidentStatus.OPEN;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // === 상태 머신 핵심 ===
    public void transitionTo(IncidentStatus next, String note) {
        if (next == null) throw new IllegalArgumentException("next status is null");
        if (this.status == next) return;

        if (!isAllowed(this.status, next)) {
            throw new IllegalStateException("Invalid transition: " + this.status + " -> " + next);
        }

        this.status = next;
        this.updatedAt = Instant.now();

        if (next == IncidentStatus.ACKNOWLEDGED) {
            this.acknowledgedAt = Instant.now();
        }
        if (next == IncidentStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
            this.resolutionNote = (note == null || note.isBlank()) ? this.resolutionNote : note;
        }
        if (next == IncidentStatus.IGNORED) {
            this.resolutionNote = (note == null || note.isBlank()) ? "ignored" : note;
        }
    }

    private boolean isAllowed(IncidentStatus from, IncidentStatus to) {
        return switch (from) {
            case OPEN -> (to == IncidentStatus.ANALYZING
                    || to == IncidentStatus.ACKNOWLEDGED
                    || to == IncidentStatus.RESOLVED
                    || to == IncidentStatus.IGNORED);

            case ANALYZING -> (to == IncidentStatus.ANALYZED
                    || to == IncidentStatus.IGNORED);

            case ANALYZED -> (to == IncidentStatus.ACKNOWLEDGED
                    || to == IncidentStatus.RESOLVED
                    || to == IncidentStatus.IGNORED);

            case ACKNOWLEDGED -> (to == IncidentStatus.RESOLVED
                    || to == IncidentStatus.IGNORED);

            case RESOLVED, IGNORED -> false; // 종결 상태는 더 못 감
        };
    }

    public void recordOccurrence(Instant occurredAt) {
        this.occurrenceCount = this.occurrenceCount + 1;
        if (this.lastSeenAt == null || occurredAt.isAfter(this.lastSeenAt)) {
            this.lastSeenAt = occurredAt;
        }
        this.updatedAt = Instant.now();
    }

    // getters (필요한 것만)
    public Long getId() { return id; }
    public IncidentStatus getStatus() { return status; }
    public String getTraceId() { return traceId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getServiceName() { return serviceName; }
    public String getSignatureHash() { return signatureHash; }
    public String getExceptionClass() { return exceptionClass; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public int getOccurrenceCount() { return occurrenceCount; }
    public String getPrimaryTraceId() { return primaryTraceId; }
    public String getSampleMessage() { return sampleMessage; }
}
