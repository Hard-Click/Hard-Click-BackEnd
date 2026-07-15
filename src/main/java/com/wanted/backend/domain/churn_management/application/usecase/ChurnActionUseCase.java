package com.wanted.backend.domain.churn_management.application.usecase;

/**
 * 이탈 위험 학생에 대한 관리자 액션(화면4의 '독려 알림 보내기' / '스케줄 재조정 권유').
 * 둘 다 실제 실행이 아니라 학생에게 알림을 발송하는 동작이다(스케줄 재계산 자체는 야간/주간 배치 소관).
 */
public interface ChurnActionUseCase {

    /** 독려 알림 발송. */
    void nudge(Long enrollmentId);

    /** 스케줄 재조정 권유 알림 발송. */
    void suggestReflow(Long enrollmentId);
}
