package com.troubleshoot.observability.domain.incident.infra;

import com.troubleshoot.observability.domain.incident.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
}
