1. 이번에 만든 것
2. 왜 만들었는지
3. 현재 어디까지 동작하는지
4. 다음에 할 것
이 형태로 적을것

## 2026-03-08

    1. Analyzer 레이어 구성
        -Incident를 분석해서 category, severity, title, summary, nextActions를 생성하는 구조 추가ㅣ
        -Analyzer 인터페이스와 RuleBasedAnalyzer 분리
    
       2. AnalysisService 구성
           - 수동 분석 흐름을 담당하는 서비스 추가
           - incident / incidentEvent 조회
           - analyzer 호출
           - 분석 결과 JSON 변환
           - IncidentAnalysis 저장 및 상태 변경 처리
        
    현재 상태
      - 분석 로직과 서비스 워크플로우까지 구성됨
        - DB 저장까지 서비스 레벨에서는 연결된 상태
    
    다음 할 일
      - analyze API 추가
        - analysis 조회 API 추가
        - 테스트 에러를 넣어서 실제 DB 저장 확인
        - 테스트 코드 작성

## 2026-03-18

    ### Incident Analysis MVP 진행
    
    #### 1. persistence layer 추가
    - `IncidentAnalysis` 엔티티 추가
      - `IncidentAnalysisRepository` 추가
      - incident 1건당 analysis 1건을 저장하는 구조로 설계
      - `incident_analysis` 테이블에 분석 결과를 저장할 수 있도록 구성
    
    #### 2. analyzer layer 추가
    - `IncidentAnalyzer` 인터페이스 추가
      - `IncidentAnalysisResult` 추가
      - `RuleBasedIncidentAnalyzer` 추가
      - incident + incident events를 기반으로 아래 분석 결과를 생성하도록 구현
          - category
          - severity
          - title
          - summary
          - keyEvidence
          - suspectedRootCauses
          - nextActions
          - analyzedAt
          - analyzerVersion
    
    #### 3. analysis service workflow 추가
    - `IncidentAnalysisService` 추가
      - 수동 분석 유스케이스 구현
      - 주요 흐름:
          - incident 조회
          - 최근 incident events 조회
          - analyzer 실행
          - `IncidentAnalysisResult`를 `IncidentAnalysis`로 변환
          - 분석 결과 저장 또는 갱신
          - incident 상태 전이 (`ANALYZING -> ANALYZED`)
          - `STATUS_CHANGED` 이벤트 기록
    
    #### 4. 상태머신 보완
    - 분석 흐름을 위해 전이 규칙 보완
      - 재분석과 분석 중 해결 가능성을 고려해 전이 허용 범위 조정
    
    #### 5. controller / API 추가
    - 수동 분석 API 추가
        - `POST /api/incidents/{id}/analyze`
      - 분석 결과 조회 API 추가
          - `GET /api/incidents/{id}/analysis`
    
    #### 6. 실제 동작 확인
    - `POST /api/incidents/10/analyze` 호출 성공
      - incident 상태가 `OPEN`에서 `ANALYZED`로 변경됨
      - `incident_analysis` 테이블에 분석 결과 저장 확인
      - 저장 확인 예시
          - incident_id = 10
          - category = DB
          - severity = LOW
          - analyzer_version = rule-v1
    
    #### 7. 이슈 및 해결
    - `GET /api/incidents/{id}/analysis` 호출 시 LAZY loading / no session 문제 발생
      - 원인:
          - `IncidentAnalysis -> Incident` 연관 객체를 세션 밖에서 접근
      - 해결 방향:
          - `@EntityGraph(attributePaths = "incident")` 또는 fetch 전략 보완 필요
    
    ### 현재 상태
    - manual analyze MVP의 핵심 흐름은 연결 완료
      - incident 분석 및 저장까지 동작 확인
      - analysis 조회 API는 LAZY loading 이슈 점검 필요
    
    ### 배운 점
    - analyzer와 service를 분리하면 책임이 명확해짐
      - analyzer는 순수 분석, service는 유스케이스 orchestration 담당
      - JPA LAZY loading은 controller/DTO 변환 시점에서 자주 문제를 일으킬 수 있음