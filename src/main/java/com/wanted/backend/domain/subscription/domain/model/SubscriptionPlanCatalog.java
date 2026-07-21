package com.wanted.backend.domain.subscription.domain.model;

import java.util.List;

/**
 * FLOWN 연간 패스 상품 카탈로그.
 * 현재 단일 상품만 존재하므로 별도 subscription_plans 테이블 조회 없이 고정값으로 관리한다.
 * (추후 다중 플랜 도입 시 DB 조회로 전환)
 * <p>
 * 가격은 고정값이 아니라 {@link com.wanted.backend.domain.subscription.application.pricing.SubscriptionPricingPolicy}
 * 가 수능 D-day 기반으로 동적 계산한다.
 */
public final class SubscriptionPlanCatalog {

    public static final Long ANNUAL_PASS_PLAN_ID = 1L;
    public static final String ANNUAL_PASS_NAME = "FLOWN 연간 패스";
    public static final int ANNUAL_PASS_DURATION_DAYS = 365;
    public static final String DEFAULT_PAYMENT_METHOD = "card";

    public static final List<String> ANNUAL_PASS_BENEFITS = List.of(
            "모든 유료 강의 수강 가능",
            "신규 강의 추가 시 자동 이용 가능",
            "학습 진도율 저장",
            "퀴즈 응시 가능",
            "마이페이지 학습 통계 반영",
            "AI 학습 스케줄러 이용 가능"
    );

    private SubscriptionPlanCatalog() {
    }
}
