# Observability Incident Analysis MVP

## 개요
이 프로젝트는 로그/에러 이벤트를 수집하고, 이를 incident 단위로 그룹핑한 뒤, 분석 결과를 저장/조회할 수 있는 Observability 실험 프로젝트이다.

## 현재 아키텍처

### 1. Error ingestion
- `POST /api/error-events`
- 에러 이벤트를 수집하고 signature 기반으로 grouping

### 2. Incident grouping
- 동일한 service + signatureHash 조합을 기준으로 incident 생성 또는 기존 incident에 누적
- incident 발생 횟수(`occurrenceCount`)와 최근 발생 시각(`lastSeenAt`) 관리

### 3. Incident detail
- `GET /api/incidents/{id}`
- incident 기본 정보와 관련 event 목록 조회

### 4. Incident analysis
- `POST /api/incidents/{id}/analyze`
- 특정 incident를 수동 분석
- analyzer가 분석 결과를 생성하고 DB에 저장

### 5. Analysis read
- `GET /api/incidents/{id}/analysis`
- 저장된 분석 결과 조회

## 주요 구성 요소

### Incident
장애 자체를 나타내는 aggregate root  
예: serviceName, exceptionClass, occurrenceCount, firstSeenAt, lastSeenAt, status

### IncidentEvent
incident에 연결된 개별 이벤트 기록  
예: INCIDENT_CREATED, EVENT_INGESTED, STATUS_CHANGED

### IncidentAnalysis
분석 결과를 저장하는 엔티티  
예: category, severity, title, summary, analyzedAt, analyzerVersion

### IncidentAnalyzer
incident를 해석해 구조화된 분석 결과를 생성하는 인터페이스

### RuleBasedIncidentAnalyzer
현재 사용 중인 deterministic analyzer 구현체  
향후 LLM/AI analyzer로 교체 가능

### IncidentAnalysisService
분석 유스케이스 orchestration 담당
- incident 조회
- events 조회
- analyzer 호출
- analysis 저장
- 상태 전이
- 상태 변경 이벤트 기록

## 설계 원칙
- analyzer와 service의 책임 분리
- 분석 결과 모델과 저장 엔티티 분리
- MVP 단계에서는 deterministic rule 기반 분석 유지
- 추후 AI/LLM analyzer로 교체 가능한 구조 유지

## 현재 한계
- analysis 조회 시 LAZY loading 문제 보완 필요
- 테스트 코드 미구현
- 자동 분석 트리거 및 리포트 기능 미구현