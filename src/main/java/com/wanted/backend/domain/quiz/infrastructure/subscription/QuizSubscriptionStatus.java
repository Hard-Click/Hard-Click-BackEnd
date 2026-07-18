package com.wanted.backend.domain.quiz.infrastructure.subscription;

// subscriptions.status 참조용(quiz 도메인 로컬). 소스 오브 트루스는 subscription 도메인의
// SubscriptionStatus(ACTIVE/CANCELLED/EXPIRED)이며, 도메인 경계 유지를 위해 별도 정의한다.
// DB에 존재하는 모든 상태값을 미러링해야 엔티티 로딩 시 enum 매핑 실패가 없다.
public enum QuizSubscriptionStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED
}
