# TODO

## Immediate
- [ ] `GET /api/incidents/{id}/analysis` LAZY loading 문제 해결
- [ ] `IncidentAnalysisRepository` 조회 방식 보완 (`@EntityGraph` 또는 fetch join)
- [ ] Swagger / 브라우저 / curl 기준으로 analysis 조회 최종 확인

## Tests
- [ ] `RuleBasedIncidentAnalyzer` 단위 테스트 추가
- [ ] `IncidentAnalysisService` 테스트 추가
- [ ] `IncidentAnalysisController` MockMvc 테스트 추가

## API / Exception handling
- [ ] controller 예외 처리 방식 정리
- [ ] 404 / 409 응답 정책 명확화
- [ ] analysis response shape 최종 확정

## Documentation
- [ ] README 정리
- [ ] architecture 메모 정리
- [ ] dev-log 누적 정리

## Next feature
- [ ] incident 분석 자동 트리거 기준 설계
- [ ] threshold 기반 report candidate 설계
- [ ] 추천안(nextActions)을 runbook/action candidate로 확장할 구조 설계