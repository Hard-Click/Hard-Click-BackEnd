package com.wanted.backend.domain.student_onboarding.domain.model;

/** 탐구 계열. 온보딩 1단계 입력값 - student_profile.exploration_track. */
public enum ExplorationTrack {

    /** 사회탐구 - 화면 기본 선택값 */
    SOCIAL,

    /** 과학탐구 */
    SCIENCE,

    /** 혼합 - 탐구1/탐구2를 사탐·과탐에서 각각 고르는 경우 */
    MIXED
}
