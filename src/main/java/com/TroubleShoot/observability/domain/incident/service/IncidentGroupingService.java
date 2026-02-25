package com.troubleshoot.observability.domain.incident.service;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import com.troubleshoot.observability.domain.incident.grouping.ExceptionSignature;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentGroupingService {

    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;

    public IncidentGroupingService(IncidentRepository incidentRepository,
                                   IncidentEventRepository incidentEventRepository) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
    }

    @Transactional
    public Incident handle(ErrorEvent errorEvent) {
        Objects.requireNonNull(errorEvent, "errorEvent");

        ExceptionSignature signature = ExceptionSignature.fromStacktrace(errorEvent.getStacktrace());
        String signatureHash = signature.getSignatureHash();

        Instant threshold = errorEvent.getOccurredAt().minus(WINDOW);

        Optional<Incident> existing = incidentRepository
                .findFirstByServiceNameAndSignatureHashAndStatusAndLastSeenAtAfter(
                        errorEvent.getServiceName(),
                        signatureHash,
                        IncidentStatus.OPEN,
                        threshold
                );

        if (existing.isPresent()) {
            Incident incident = existing.get();
            incident.recordOccurrence(errorEvent.getOccurredAt());
            incidentEventRepository.save(new IncidentEvent(
                    incident,
                    IncidentEventType.EVENT_INGESTED,
                    null,
                    errorEvent.getOccurredAt(),
                    errorEvent.getTraceId(),
                    errorEvent.getMessage()
            ));
            return incident;
        }

        String sampleMessage = normalizeOptional(errorEvent.getMessage());
        String primaryTraceId = normalizeOptional(errorEvent.getTraceId());

        Incident incident = new Incident(
                errorEvent.getServiceName(),
                signatureHash,
                errorEvent.getExceptionClass(),
                errorEvent.getOccurredAt(),
                primaryTraceId,
                sampleMessage
        );

        incident = incidentRepository.save(incident);

        incidentEventRepository.save(new IncidentEvent(
                incident,
                IncidentEventType.INCIDENT_CREATED,
                null,
                errorEvent.getOccurredAt(),
                errorEvent.getTraceId(),
                errorEvent.getMessage()
        ));

        return incident;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
