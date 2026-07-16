package com.wanted.backend.domain.student_onboarding.domain.model;

/**
 * 학습 성향. 온보딩 1단계 입력값 - student_profile.study_preference.
 *
 * <p>⚠️ 현재 CP-SAT 스케줄러는 이 값을 읽지 않는다. 하루 안에서 슬롯을 어느 시간대에 배치할지
 * 결정하는 입력으로 쓸 수 있으나, 아직 배치 로직에 연결돼 있지 않다(수집만).
 */
public enum StudyPreference {

    /** 아침형 */
    MORNING,

    /** 저녁형 */
    EVENING,

    /** 무관 - 화면 기본 선택값 */
    NONE
}
