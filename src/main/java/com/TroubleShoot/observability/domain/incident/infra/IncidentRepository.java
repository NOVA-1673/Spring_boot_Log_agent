package com.troubleshoot.observability.domain.incident.infra;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    java.util.Optional<Incident> findFirstByServiceNameAndSignatureHashAndStatusAndLastSeenAtAfter(
            String serviceName,
            String signatureHash,
            IncidentStatus status,
            java.time.Instant threshold
    );
}
