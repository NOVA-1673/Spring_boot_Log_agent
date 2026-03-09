package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventRepository;
import com.troubleshoot.observability.domain.incident.service.ErrorEvent;
import com.troubleshoot.observability.domain.incident.service.IncidentGroupingService;
import com.troubleshoot.observability.domain.incident.service.IncidentService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class IncidentController {

    private final IncidentService service;
    private final IncidentGroupingService groupingService;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;



    public IncidentController(IncidentService service,
                              IncidentGroupingService groupingService,
                              IncidentRepository incidentRepository,
                              IncidentEventRepository incidentEventRepository) {
        this.service = service;
        this.groupingService = groupingService;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
    }

    public static record ChangeStatusRequest(IncidentStatus status, String note) {}

    @PatchMapping("/incidents/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable long id,
                                          @RequestBody ChangeStatusRequest req) {
        Incident updated = service.changeStatus(id, req.status(), req.note());
        return ResponseEntity.ok(updated.getStatus());
    }

    @PostMapping("/api/error-events")
    public ResponseEntity<ErrorEventResponse> ingest(@Valid @RequestBody ErrorEventRequest req) {
        ErrorEvent event = new ErrorEvent(
                req.serviceName(),
                req.occurredAt(),
                req.traceId(),
                req.message(),
                req.exceptionClass(),
                req.stacktrace()
        );

        Incident incident = groupingService.handle(event);
        boolean grouped = incident.getOccurrenceCount() > 1; // occurrenceCount == 1 means newly created

        return ResponseEntity.ok(new ErrorEventResponse(
                incident.getId(),
                incident.getStatus(),
                grouped
        ));
    }

    @GetMapping("/api/incidents")
    public ResponseEntity<List<IncidentSummaryResponse>> listIncidents(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        List<Incident> incidents = findIncidents(serviceName, status, from, to);
        List<IncidentSummaryResponse> response = incidents.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/incidents/{id}")
    public ResponseEntity<IncidentDetailResponse> getIncident(@PathVariable long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "incident not found: " + id));

        List<IncidentEvent> events = incidentEventRepository.findByIncidentIdOrderByOccurredAtAsc(id);

        List<IncidentEventSummary> eventResponses = events.stream()
                .map(event -> new IncidentEventSummary(
                        event.getId(),
                        event.getType(),
                        event.getOccurredAt(),
                        event.getTraceId(),
                        event.getMessage(),
                        event.getNote()
                ))
                .collect(Collectors.toList());

        IncidentDetailResponse detail = new IncidentDetailResponse(
                incident.getId(),
                incident.getStatus(),
                incident.getServiceName(),
                incident.getExceptionClass(),
                incident.getOccurrenceCount(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),
                eventResponses
        );

        return ResponseEntity.ok(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Void> handleValidationFailure(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().build();
    }

    private IncidentSummaryResponse toSummary(Incident incident) {
        return new IncidentSummaryResponse(
                incident.getId(),
                incident.getStatus(),
                incident.getServiceName(),
                incident.getExceptionClass(),
                incident.getOccurrenceCount(),
                incident.getLastSeenAt()
        );
    }

    private List<Incident> findIncidents(String serviceName,
                                         IncidentStatus status,
                                         Instant from,
                                         Instant to) {
        if (serviceName != null && status != null) {
            if (from != null && to != null) {
                return incidentRepository
                        .findByServiceNameAndStatusAndLastSeenAtBetweenOrderByLastSeenAtDesc(
                                serviceName, status, from, to);
            }
            if (from != null) {
                return incidentRepository
                        .findByServiceNameAndStatusAndLastSeenAtAfterOrderByLastSeenAtDesc(
                                serviceName, status, from);
            }
            if (to != null) {
                return incidentRepository
                        .findByServiceNameAndStatusAndLastSeenAtBeforeOrderByLastSeenAtDesc(
                                serviceName, status, to);
            }
            return incidentRepository.findByServiceNameAndStatusOrderByLastSeenAtDesc(
                    serviceName, status);
        }

        if (serviceName != null) {
            if (from != null && to != null) {
                return incidentRepository
                        .findByServiceNameAndLastSeenAtBetweenOrderByLastSeenAtDesc(
                                serviceName, from, to);
            }
            if (from != null) {
                return incidentRepository
                        .findByServiceNameAndLastSeenAtAfterOrderByLastSeenAtDesc(serviceName, from);
            }
            if (to != null) {
                return incidentRepository
                        .findByServiceNameAndLastSeenAtBeforeOrderByLastSeenAtDesc(serviceName, to);
            }
            return incidentRepository.findByServiceNameOrderByLastSeenAtDesc(serviceName);
        }

        if (status != null) {
            if (from != null && to != null) {
                return incidentRepository
                        .findByStatusAndLastSeenAtBetweenOrderByLastSeenAtDesc(status, from, to);
            }
            if (from != null) {
                return incidentRepository
                        .findByStatusAndLastSeenAtAfterOrderByLastSeenAtDesc(status, from);
            }
            if (to != null) {
                return incidentRepository
                        .findByStatusAndLastSeenAtBeforeOrderByLastSeenAtDesc(status, to);
            }
            return incidentRepository.findByStatusOrderByLastSeenAtDesc(status);
        }

        if (from != null && to != null) {
            return incidentRepository.findByLastSeenAtBetweenOrderByLastSeenAtDesc(from, to);
        }
        if (from != null) {
            return incidentRepository.findByLastSeenAtAfterOrderByLastSeenAtDesc(from);
        }
        if (to != null) {
            return incidentRepository.findByLastSeenAtBeforeOrderByLastSeenAtDesc(to);
        }
        return incidentRepository.findAllByOrderByLastSeenAtDesc();
    }
}
