package com.troubleshoot.observability.domain.incident.service;


import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentWriter {

    private final IncidentRepository repo;

    public IncidentWriter(IncidentRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Incident incident) {
        repo.save(incident);
    }

}
