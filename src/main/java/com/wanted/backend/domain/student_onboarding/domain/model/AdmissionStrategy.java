package com.wanted.backend.domain.student_onboarding.domain.model;

/** 입시 전략. 온보딩 1단계 입력값 - student_profile.admission_strategy. */
public enum AdmissionStrategy {

    /** 정시 위주 */
    REGULAR,

    /** 수시 위주 */
    EARLY,

    /** 병행 / 미정 - 화면 기본 선택값 */
    UNDECIDED
}
