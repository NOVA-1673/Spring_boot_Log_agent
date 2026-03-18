package com.troubleshoot.observability.domain.incident.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, Long> {

    @EntityGraph(attributePaths = "incident")
    Optional<IncidentAnalysis> findByIncident_Id(Long incidentId);
}
