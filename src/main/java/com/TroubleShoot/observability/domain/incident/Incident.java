package com.troubleshoot.observability.domain.incident;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident")
public class Incident {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String traceId;

    @Column(nullable = false, length = 32)
    private String category; // 예: DB, AUTH, VALIDATION, NPE ...

    @Column(nullable = false, length = 16)
    private String severity; // 예: LOW/MEDIUM/HIGH

    @Column(nullable = false, length = 16)
    private int statusCode;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Incident() {}

    public Incident(String traceId, String category, String severity,
                    int statusCode, String method, String path, String message) {
        this.traceId = traceId;
        this.category = category;
        this.severity = severity;
        this.statusCode = statusCode;
        this.method = method;
        this.path = path;
        this.message = message;
    }

    public Long getId() { return id; }
}
