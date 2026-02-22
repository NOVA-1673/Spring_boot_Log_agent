package com.troubleshoot.observability.domain.incident.api;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.IncidentStatus;
import com.troubleshoot.observability.domain.incident.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) {
        this.service = service;
    }

    public static record ChangeStatusRequest(IncidentStatus status, String note) {}

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable long id,
                                          @RequestBody ChangeStatusRequest req) {
        Incident updated = service.changeStatus(id, req.status(), req.note());
        return ResponseEntity.ok(updated.getStatus());
    }
}