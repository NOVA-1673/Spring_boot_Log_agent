package com.troubleshoot.observability.domain.incident.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {
    List<IncidentEvent> findTop50ByIncidentIdOrderByOccurredAtDesc(Long incidentId);
}
