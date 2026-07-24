package com.wanted.backend.domain.quiz.application.port;

import java.time.LocalDateTime;

/**
 * 복습(FSRS review_card) 완료 처리 아웃바운드 포트.
 *
 * <p>유사퀴즈 제출이 곧 그 코스의 '오늘 복습 완료'다. 완료 시 해당 코스의 오늘까지 due 인 복습 카드를
 * 다음 주기로 전진시키면, 캘린더의 복습 항목은 due 가 미래로 밀려 오늘 목록에서 빠진다
 * (study_schedule 이 REVIEW 를 항상 PLANNED 로 노출하고 진행률 분모에서 제외하는 설계와 정합).
 *
 * <p>도메인 결합을 인프라로 격리하기 위해 포트는 quiz 가 소유하고, 구현체(어댑터)가 review_card 를
 * 직접 갱신한다(SimilarQuizSubscriptionAccessPort 와 동일한 아웃바운드 패턴).
 */
public interface ReviewCompletionPort {

    /**
     * 회원의 특정 코스에서 오늘까지 due 인 복습 카드를 완료 처리(다음 주기로 전진)한다.
     * 오늘 due 인 카드가 없으면 아무것도 하지 않는다(no-op). 이미 완료돼 due 가 미래인 카드는 대상이 아니므로,
     * 같은 날 재제출해도 이중 전진하지 않는다(멱등).
     *
     * @return 전진된 카드 수(0 이면 완료 대상 없음)
     */
    int completeTodayReviews(Long memberId, Long courseId, LocalDateTime completedAt);
}
