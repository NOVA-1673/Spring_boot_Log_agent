1. 이번에 만든 것
2. 왜 만들었는지
3. 현재 어디까지 동작하는지
4. 다음에 할 것
이 형태로 적을것

2026-03-08

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