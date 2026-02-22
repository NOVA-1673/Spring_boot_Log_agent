package com.troubleshoot.observability.domain.incident.service;

import com.troubleshoot.observability.domain.incident.*;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public class IncidentService {
    private final IncidentRepository repo;

    public IncidentService(IncidentRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Incident changeStatus(long id, IncidentStatus next, String note) {
        Incident inc = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("incident not found: " + id));

        inc.transitionTo(next, note); // ✅ 상태머신 규칙 강제
        return inc; // dirty checking으로 UPDATE
    }
}
