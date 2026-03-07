package com.troubleshoot.observability.domain.incident.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, Long> {

    Optional<IncidentAnalysis> findByIncident_Id(Long incidentId);
}
