package com.TroubleShoot.observability.domain.incident.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.infra.IncidentRepository;
import com.troubleshoot.observability.domain.incident.service.ErrorEvent;
import com.troubleshoot.observability.domain.incident.service.IncidentGroupingService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(IncidentGroupingService.class)
class IncidentGroupingServiceTest {

    @Autowired
    private IncidentGroupingService service;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void sameSignatureWithinWindowUpdatesSameIncident() {
        Instant t0 = Instant.parse("2026-02-23T10:00:00Z");

        Incident first = service.handle(event(
                "billing",
                t0,
                "trace-1",
                "msg-1",
                "java.lang.IllegalStateException",
                stacktraceA()
        ));

        Incident second = service.handle(event(
                "billing",
                t0.plus(Duration.ofMinutes(4)),
                "trace-2",
                "msg-2",
                "java.lang.IllegalStateException",
                stacktraceA()
        ));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getOccurrenceCount()).isEqualTo(2);
        assertThat(second.getLastSeenAt()).isEqualTo(t0.plus(Duration.ofMinutes(4)));
        assertThat(incidentRepository.count()).isEqualTo(1);
    }

    @Test
    void sameSignatureAfterWindowCreatesNewIncident() {
        Instant t0 = Instant.parse("2026-02-23T10:00:00Z");

        Incident first = service.handle(event(
                "billing",
                t0,
                "trace-1",
                "msg-1",
                "java.lang.IllegalStateException",
                stacktraceA()
        ));

        Incident second = service.handle(event(
                "billing",
                t0.plus(Duration.ofMinutes(6)),
                "trace-2",
                "msg-2",
                "java.lang.IllegalStateException",
                stacktraceA()
        ));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(incidentRepository.count()).isEqualTo(2);
    }

    @Test
    void differentSignatureCreatesNewIncident() {
        Instant t0 = Instant.parse("2026-02-23T10:00:00Z");

        Incident first = service.handle(event(
                "billing",
                t0,
                "trace-1",
                "msg-1",
                "java.lang.IllegalStateException",
                stacktraceA()
        ));

        Incident second = service.handle(event(
                "billing",
                t0.plus(Duration.ofMinutes(2)),
                "trace-2",
                "msg-2",
                "java.lang.IllegalArgumentException",
                stacktraceB()
        ));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(incidentRepository.count()).isEqualTo(2);
    }

    private static ErrorEvent event(String serviceName,
                                    Instant occurredAt,
                                    String traceId,
                                    String message,
                                    String exceptionClass,
                                    String stacktrace) {
        return new ErrorEvent(serviceName, occurredAt, traceId, message, exceptionClass, stacktrace);
    }

    private static String stacktraceA() {
        return String.join("\n",
                "java.lang.IllegalStateException: boom",
                "    at com.acme.Foo.bar(Foo.java:10)",
                "    at com.acme.Baz.bat(Baz.java:20)"
        );
    }

    private static String stacktraceB() {
        return String.join("\n",
                "java.lang.IllegalArgumentException: bad input",
                "    at com.acme.Other.doWork(Other.java:33)",
                "    at com.acme.Helper.run(Helper.java:44)"
        );
    }
}
