package com.wanted.backend.domain.quiz.application.port;

/**
 * 회원의 유효 구독 여부 확인 아웃바운드 포트.
 * 유사퀴즈는 구독 회원 전용 — 생성(①)/제출(②) 진입 게이트로 사용한다.
 */
public interface SimilarQuizSubscriptionAccessPort {

    boolean hasActiveSubscription(Long memberId);
}
