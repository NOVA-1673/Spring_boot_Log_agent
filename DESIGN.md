# DESIGN.md — Incident Grouping Architecture

## 1. Goal

Reduce noisy error events into structured Incidents
that can later be analyzed by AI.

---

## 2. Problem

Without grouping:

- Every exception becomes a new alert
- Duplicate stacktraces create noise
- AI cost increases exponentially
- Hard to track lifecycle of issues

---

## 3. Architecture Overview

ErrorEvent
↓
ExceptionSignature
↓
Grouping Key (service + signatureHash)
↓
Time Window Check
↓
Incident (create or update)

---

## 4. Core Concepts

### 4.1 ErrorEvent

Represents a single occurrence of an exception.

Fields:
- serviceName
- timestamp
- exceptionClass
- stacktrace
- traceId (optional)

---

### 4.2 Exception Signature

Definition:
signature = exceptionClass + normalized top N stack frames

Normalization:
- keep first 5 frames
- remove JDK internal frames (optional)
- configurable includeLineNumber flag

signatureHash = SHA-256(signatureString)

Purpose:
Identify identical root causes.

---

### 4.3 Grouping Rule (MVP)

key = (serviceName, signatureHash)

timeWindow = 5 minutes

IF:
existing OPEN incident with same key
AND lastSeenAt within window

THEN:
update incident (increment count, update lastSeenAt)

ELSE:
create new incident

---

## 5. Incident Lifecycle

OPEN
↓
ANALYZING (future)
↓
ANALYZED (future)
↓
RESOLVED / IGNORED

---

## 6. Why This Matters for AI

AI should analyze:
- 1 incident (representative sample)
  NOT:
- 500 duplicated logs

This dramatically reduces:
- token usage
- hallucination risk
- noise

---

## 7. Future Extensions

- Signature similarity clustering
- Dynamic window size
- Severity scoring
- AI-assisted merge suggestion
- Cross-service correlation