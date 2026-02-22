package com.troubleshoot.observability.domain.incident;

public enum IncidentStatus {
    OPEN,          // 생성됨(미처리)
    ANALYZING,     // AI 분석 중
    ANALYZED,      // AI 분석 완료
    ACKNOWLEDGED,  // 운영자 확인
    RESOLVED,      // 해결됨
    IGNORED        // 무시/오탐
}
