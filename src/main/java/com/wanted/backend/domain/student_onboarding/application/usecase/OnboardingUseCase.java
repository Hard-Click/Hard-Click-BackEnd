package com.wanted.backend.domain.student_onboarding.application.usecase;

import com.wanted.backend.domain.student_onboarding.application.dto.OnboardingDtos;

/**
 * 학습 스케줄 온보딩 - 구독권 결제 완료 직후 3개 화면을 순서대로 저장한다.
 *
 * <p>각 단계는 덮어쓰기(upsert)다 - 화면을 되돌아와 다시 저장할 수 있다.
 * 3단계까지 끝나면 student_capacity.onboarded_at 이 찍히고 스케줄러가 콜드스타트 폴백을 벗어난다.
 */
public interface OnboardingUseCase {

    /** 1단계 - 목표/입시전략/선택과목/학습성향 저장. */
    void saveProfile(Long memberId, OnboardingDtos.SaveProfileCommand command);

    /** 2단계 - 불가능 시간 저장. 가용 구간·쉬는 날·하루 상한이 함께 갱신된다. */
    void saveAvailability(Long memberId, OnboardingDtos.SaveAvailabilityCommand command);

    /** 3단계 - 모의고사 원점수 저장(등급 변환 포함). 저장되면 온보딩 완료로 표시한다. */
    void saveExamScores(Long memberId, OnboardingDtos.SaveExamScoresCommand command);

    /** 진행 상태 조회 - 재진입 시 어느 단계부터 이어갈지 판단용. */
    OnboardingDtos.OnboardingStatusView getStatus(Long memberId);
}
