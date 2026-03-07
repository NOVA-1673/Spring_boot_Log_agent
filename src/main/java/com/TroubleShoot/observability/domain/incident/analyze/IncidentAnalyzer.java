package com.troubleshoot.observability.domain.incident.analyze;

import com.troubleshoot.observability.domain.incident.Incident;
import com.troubleshoot.observability.domain.incident.persistence.IncidentEvent;
import java.util.List;

public interface IncidentAnalyzer {

    IncidentAnalysisResult analyze(Incident incident, List<IncidentEvent> events);
}
