package com.troubleshoot.observability.domain.incident.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);

    @GetMapping
    public String health() {
        log.info("health check called");
        return "OK";
    }
}
