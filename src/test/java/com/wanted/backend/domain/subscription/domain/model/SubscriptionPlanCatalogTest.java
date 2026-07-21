package com.wanted.backend.domain.subscription.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FLOWN 연간 패스 혜택 목록 회귀 방지.
 * FE는 이 목록을 필터링 없이 그대로 렌더링하므로, 항목/순서가 계약이다.
 * 'AI 학습 스케줄러 이용 가능'이 다시 누락되면 실패한다.
 */
class SubscriptionPlanCatalogTest {

    private static final String AI_SCHEDULER_BENEFIT = "AI 학습 스케줄러 이용 가능";

    @Test
    @DisplayName("연간 패스 혜택은 6개이며 AI 학습 스케줄러 항목을 포함한다")
    void benefitsIncludeAiScheduler() {
        assertThat(SubscriptionPlanCatalog.ANNUAL_PASS_BENEFITS)
                .hasSize(6)
                .contains(AI_SCHEDULER_BENEFIT);
    }

    @Test
    @DisplayName("혜택 목록의 항목/순서는 FE 계약과 일치한다")
    void benefitsMatchFrontendContract() {
        assertThat(SubscriptionPlanCatalog.ANNUAL_PASS_BENEFITS)
                .containsExactly(
                        "모든 유료 강의 수강 가능",
                        "신규 강의 추가 시 자동 이용 가능",
                        "학습 진도율 저장",
                        "퀴즈 응시 가능",
                        "마이페이지 학습 통계 반영",
                        AI_SCHEDULER_BENEFIT
                );
    }
}
