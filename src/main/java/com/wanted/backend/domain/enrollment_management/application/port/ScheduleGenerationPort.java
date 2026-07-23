package com.wanted.backend.domain.enrollment_management.application.port;

/**
 * AI 스케줄러(Python)에 해당 회원의 주간 스케줄 즉시 생성을 요청하는 출력 포트.
 * 실패해도 수강신청 결과에 영향을 주면 안 되는 부가 작업이다(다음 주간 배치가 보정).
 */
public interface ScheduleGenerationPort {

    void requestGeneration(Long memberId);
}
