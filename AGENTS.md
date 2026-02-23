# AGENTS.md

## Project Vision

Build an AI-powered Observability platform that
automatically groups, analyzes, and resolves incidents.

---

## Development Rules (Persistent)

- Always add unit tests for new logic.
- Keep grouping logic deterministic (no LLM in Phase 1).
- Prefer small, reviewable commits.
- Do not break existing public APIs.
- Follow Spring layered architecture.
- Avoid over-engineering in MVP.
- Document non-obvious logic.

---

## Current Phase

Phase 1: Incident Grouping MVP

### Objective 1 — ExceptionSignature (deterministic)
Implement `ExceptionSignature` that can be created from either:
- `Throwable` (preferred) OR
- a raw stacktrace string (fallback)

**Signature string definition**
- signatureString = exceptionClassName + "\n" + join(normalizedFrames[0..N-1], "\n")
- N (topFrames) default = 5 (configurable via constructor or properties)

**Frame normalization rules**
- For each StackTraceElement:
    - normalize as: `className#methodName(fileName:line)` if fileName & lineNumber available
    - if includeLineNumber=false: use `className#methodName(fileName)`
- Optional filtering: drop frames whose className starts with any prefix:
    - `java.`, `jdk.`, `sun.`, `org.springframework.`
    - Make this configurable (default ON for JDK, OFF for Spring)
- Only include frames after filtering; if fewer than N remain, include all remaining.

**Hash**
- signatureHash = SHA-256(signatureString) as lowercase hex
- Must be stable across runs.

**Tests**
- Unit tests that verify:
    - same Throwable produces the same hash
    - toggling includeLineNumber changes/doesn't change hash as expected
    - filtering removes JDK frames
    - for Throwable input: use the root cause exceptionClassName by default
      (no deep cause-chain parsing in Phase 1)

---

### Objective 2 — IncidentGroupingService (window-based, deterministic)
Implement `IncidentGroupingService` to assign incoming `ErrorEvent` to an `Incident`.

**Input DTO/domain object**
`ErrorEvent`:
- serviceName (string, required)
- occurredAt (Instant, required)
- traceId (string, optional)
- message (string, optional)
- exceptionClass (string, required)
- stacktrace (string, required)
- signatureHash (derived via ExceptionSignature)

**Grouping key**
- groupingKey = (serviceName, signatureHash)

**Time window**
- window = 5 minutes (configurable, default 5m)

**Grouping rule**
- Find an existing Incident where:
    - status IN (OPEN, ANALYZING) (configurable list)
    - groupingKey matches
    - lastSeenAt >= occurredAt - window
- If found:
    - increment `occurrenceCount`
    - set `lastSeenAt = max(lastSeenAt, occurredAt)`
    - append a new IncidentEvent with type `EVENT_INGESTED`
- Else:
    - create new Incident with:
        - status = OPEN
        - firstSeenAt = occurredAt
        - lastSeenAt = occurredAt
        - occurrenceCount = 1
        - primaryTraceId = traceId (if present)
        - sampleMessage = message (first message)
        - signatureHash = signatureHash
        - exceptionClass = exceptionClass
    - append IncidentEvent `INCIDENT_CREATED`

**Concurrency**
- Must avoid creating duplicate incidents under concurrent ingest.
- Use one of:
    - DB unique constraint + retry OR
    - SELECT ... FOR UPDATE on an "active incident" query OR
    - optimistic locking (@Version) with retry
      (Choose the simplest for MVP; document the choice.)

**Tests**
- Unit tests: same key within window -> same incident
- Unit tests: same key outside window -> new incident
- Concurrency test (optional if hard): ensure no duplicates under parallel calls (at least document limitations)

---

### Objective 3 — Persistence Model (JPA)
Create entities & repositories.

**Incident**
Fields (minimum):
- id (PK)
- serviceName
- status (enum)
- signatureHash
- exceptionClass
- firstSeenAt
- lastSeenAt
- occurrenceCount
- primaryTraceId (nullable)
- sampleMessage (nullable)
- createdAt / updatedAt
- version (@Version) optional for optimistic locking

**IncidentEvent**
Fields:
- id (PK)
- incidentId (FK)
- type (enum: INCIDENT_CREATED, EVENT_INGESTED, STATUS_CHANGED)
- note (nullable)
- occurredAt (Instant)
- traceId (nullable)
- createdAt

**Repositories**
- IncidentRepository: query active incident by key + time window
- IncidentEventRepository: list events by incidentId

**Tests**
- JPA tests (DataJpaTest) to verify query works

---

### Objective 4 — REST API (minimal, stable)
Provide endpoints (JSON):

1) POST `/api/error-events`
- request: ErrorEventRequest (serviceName, occurredAt, traceId?, message?, exceptionClass, stacktrace)
- response: { incidentId, status, grouped: true/false }

2) GET `/api/incidents`
- query params: serviceName?, status?, from?, to?
- response: list summary (id, status, serviceName, exceptionClass, occurrenceCount, lastSeenAt)

3) GET `/api/incidents/{id}`
- response: incident detail + recent events (last 50)

**Validation**
- serviceName, occurredAt, exceptionClass, stacktrace required
- limit stacktrace size (e.g., 64KB) to protect API

**Tests**
- Controller tests (MockMvc) for validation + happy path

---

### Objective 5 — Definition of Done (Phase 1)
Phase 1 is “done” when:
- posting the same stacktrace repeatedly within 5 minutes updates the same incident and increases occurrenceCount
- posting after 5 minutes creates a new incident
- tests pass (`./gradlew test`)
- endpoints return consistent JSON


## Next Planned Phases

Phase 2: AI-based Incident Analysis
Phase 3: RAG Integration
Phase 4: Agent-driven Actions (Jira/Slack/Runbooks)