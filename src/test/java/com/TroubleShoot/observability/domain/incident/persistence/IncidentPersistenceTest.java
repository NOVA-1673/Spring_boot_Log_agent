package com.TroubleShoot.observability.domain.incident.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventType;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class IncidentPersistenceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Test
    void persistsIncidentAndEventsAndQueriesByIncidentId() {
        Incident incident = new Incident(
                "trace-123",
                "DB",
                "HIGH",
                500,
                "GET",
                "/billing/charge",
                "sample message"
        );
        incident = incidentRepository.save(incident);

        IncidentEvent older = new IncidentEvent(
                incident,
                IncidentEventType.INCIDENT_CREATED,
                "created",
                Instant.parse("2026-02-23T10:15:31Z"),
                "trace-123",
                "first event"
        );
        IncidentEvent newer = new IncidentEvent(
                incident,
                IncidentEventType.EVENT_INGESTED,
                null,
                Instant.parse("2026-02-23T10:16:31Z"),
                "trace-124",
                "second event"
        );

        incidentEventRepository.save(older);
        incidentEventRepository.save(newer);
        entityManager.flush();
        entityManager.clear();

        Incident reloaded = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(incident.getId());

        List<IncidentEvent> events =
                incidentEventRepository.findTop50ByIncidentIdOrderByOccurredAtDesc(incident.getId());
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getOccurredAt()).isAfter(events.get(1).getOccurredAt());

        Object statusValue = entityManager
                .createNativeQuery("select status from incident where id = :id")
                .setParameter("id", incident.getId())
                .getSingleResult();
        assertThat(statusValue).isEqualTo("OPEN");
    }
}
