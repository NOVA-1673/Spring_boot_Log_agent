package com.TroubleShoot.observability.domain.incident.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.troubleshoot.observability.ObservabilityApplication;
import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventRepository;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEventType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = ObservabilityApplication.class)
@AutoConfigureMockMvc
class IncidentControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @BeforeEach
    void resetData() {
        incidentEventRepository.deleteAll();
        incidentRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void postErrorEventsRejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/error-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postErrorEventsReturnsIncident() throws Exception {
        String payload = """
                {
                  "serviceName": "billing",
                  "occurredAt": "2026-02-25T10:15:30Z",
                  "traceId": "trace-1",
                  "message": "boom",
                  "exceptionClass": "java.lang.IllegalStateException",
                  "stacktrace": "java.lang.IllegalStateException: boom\\n\\tat com.example.Billing.charge(Billing.java:10)"
                }
                """;

        mockMvc.perform(post("/api/error-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").isNumber())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.grouped").value(false));
    }

    @Test
    void listIncidentsReturnsSortedByLastSeenAtDesc() throws Exception {
        Incident older = new Incident(
                "billing",
                "hash-1",
                "java.lang.IllegalStateException",
                Instant.parse("2026-02-25T10:10:00Z"),
                null,
                "first"
        );

        Incident newer = new Incident(
                "billing",
                "hash-2",
                "java.lang.RuntimeException",
                Instant.parse("2026-02-25T10:12:00Z"),
                null,
                "second"
        );
        newer.recordOccurrence(Instant.parse("2026-02-25T10:15:00Z"));

        Incident savedOlder = incidentRepository.save(older);
        Incident savedNewer = incidentRepository.save(newer);

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(savedNewer.getId()))
                .andExpect(jsonPath("$[1].id").value(savedOlder.getId()));
    }

    @Test
    void getIncidentReturnsDetailAndEvents() throws Exception {
        Incident incident = new Incident(
                "billing",
                "hash-9",
                "java.lang.IllegalArgumentException",
                Instant.parse("2026-02-25T10:20:00Z"),
                "trace-10",
                "sample"
        );
        incident = incidentRepository.save(incident);

        IncidentEvent older = new IncidentEvent(
                incident,
                IncidentEventType.INCIDENT_CREATED,
                "created",
                Instant.parse("2026-02-25T10:20:05Z"),
                "trace-10",
                "first"
        );
        IncidentEvent newer = new IncidentEvent(
                incident,
                IncidentEventType.EVENT_INGESTED,
                "ingested",
                Instant.parse("2026-02-25T10:21:05Z"),
                "trace-11",
                "second"
        );
        incidentEventRepository.save(older);
        incidentEventRepository.save(newer);

        mockMvc.perform(get("/api/incidents/{id}", incident.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.firstSeenAt").value("2026-02-25T10:20:00Z"))
                .andExpect(jsonPath("$.lastSeenAt").value("2026-02-25T10:20:00Z"))
                .andExpect(jsonPath("$.events", hasSize(2)))
                .andExpect(jsonPath("$.events[0].type").value("INCIDENT_CREATED"))
                .andExpect(jsonPath("$.events[0].occurredAt").value("2026-02-25T10:20:05Z"))
                .andExpect(jsonPath("$.events[1].type").value("EVENT_INGESTED"))
                .andExpect(jsonPath("$.events[1].occurredAt").value("2026-02-25T10:21:05Z"));
    }

    @Test
    void getIncidentReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/incidents/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        @Order(0)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .securityMatcher("/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.disable());
            return http.build();
        }
    }
}
