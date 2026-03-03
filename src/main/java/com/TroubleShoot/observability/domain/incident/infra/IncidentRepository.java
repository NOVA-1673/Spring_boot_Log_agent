package com.troubleshoot.observability.domain.incident.infra;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    java.util.Optional<Incident> findFirstByServiceNameAndSignatureHashAndStatusAndLastSeenAtAfter(
            String serviceName,
            String signatureHash,
            IncidentStatus status,
            java.time.Instant threshold
    );

    List<Incident> findAllByOrderByLastSeenAtDesc();

    List<Incident> findByServiceNameOrderByLastSeenAtDesc(String serviceName);

    List<Incident> findByStatusOrderByLastSeenAtDesc(IncidentStatus status);

    List<Incident> findByServiceNameAndStatusOrderByLastSeenAtDesc(String serviceName, IncidentStatus status);

    List<Incident> findByLastSeenAtBetweenOrderByLastSeenAtDesc(Instant from, Instant to);

    List<Incident> findByLastSeenAtAfterOrderByLastSeenAtDesc(Instant from);

    List<Incident> findByLastSeenAtBeforeOrderByLastSeenAtDesc(Instant to);

    List<Incident> findByServiceNameAndLastSeenAtBetweenOrderByLastSeenAtDesc(
            String serviceName,
            Instant from,
            Instant to
    );

    List<Incident> findByServiceNameAndLastSeenAtAfterOrderByLastSeenAtDesc(String serviceName, Instant from);

    List<Incident> findByServiceNameAndLastSeenAtBeforeOrderByLastSeenAtDesc(String serviceName, Instant to);

    List<Incident> findByStatusAndLastSeenAtBetweenOrderByLastSeenAtDesc(
            IncidentStatus status,
            Instant from,
            Instant to
    );

    List<Incident> findByStatusAndLastSeenAtAfterOrderByLastSeenAtDesc(IncidentStatus status, Instant from);

    List<Incident> findByStatusAndLastSeenAtBeforeOrderByLastSeenAtDesc(IncidentStatus status, Instant to);

    List<Incident> findByServiceNameAndStatusAndLastSeenAtBetweenOrderByLastSeenAtDesc(
            String serviceName,
            IncidentStatus status,
            Instant from,
            Instant to
    );

    List<Incident> findByServiceNameAndStatusAndLastSeenAtAfterOrderByLastSeenAtDesc(
            String serviceName,
            IncidentStatus status,
            Instant from
    );

    List<Incident> findByServiceNameAndStatusAndLastSeenAtBeforeOrderByLastSeenAtDesc(
            String serviceName,
            IncidentStatus status,
            Instant to
    );
}
