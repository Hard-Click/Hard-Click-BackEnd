package com.wanted.backend.domain.subscription.application.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate 1 · CRITICAL 규칙 DOM_001 — 연간 패스 가격 공식을 코드로 못박는다.
 * 명세 고정값: 가격 = (다가오는 수능까지 D-day) × 30000, 수능일 2026-11-19. (CLAUDE.md: 임의 변경 금지)
 * 주입 Clock으로 시간을 고정해 결정론 검증.
 */
class SubscriptionPricingPolicyTest {

    private static final int SPEC_DAILY_RATE = 30000;       // 명세 고정 일일 단가
    private static final String SPEC_SUNEUNG = "2026-11-19"; // 명세 수능일

    private SubscriptionPricingPolicy policyAsOf(LocalDate today) {
        Clock fixed = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        return new SubscriptionPricingPolicy(fixed, SPEC_SUNEUNG, SPEC_DAILY_RATE);
    }

    @Test
    @DisplayName("DOM_001: 가격 = (수능까지 D-day) × 30000")
    void priceIsDaysUntilSuneungTimesDailyRate() {
        SubscriptionPricingPolicy policy = policyAsOf(LocalDate.of(2026, 11, 9)); // 수능까지 D-10

        assertThat(policy.daysUntilSuneung()).isEqualTo(10);
        assertThat(policy.currentPrice()).isEqualTo(10 * SPEC_DAILY_RATE); // 300,000
    }

    @Test
    @DisplayName("DOM_001: 수능 하루 전 = D-1 → 30,000")
    void dayBeforeSuneungIsOneDailyRate() {
        assertThat(policyAsOf(LocalDate.of(2026, 11, 18)).currentPrice()).isEqualTo(SPEC_DAILY_RATE);
    }

    @Test
    @DisplayName("DOM_001: 수능 당일이면 다음 해로 롤오버 — D-day≥1, 가격은 0/음수가 되지 않는다")
    void rollsOverOnSuneungDaySoPriceNeverZeroOrNegative() {
        SubscriptionPricingPolicy policy = policyAsOf(LocalDate.of(2026, 11, 19)); // 수능 당일

        assertThat(policy.upcomingSuneungDate()).isEqualTo(LocalDate.of(2027, 11, 19));
        assertThat(policy.daysUntilSuneung()).isGreaterThanOrEqualTo(1);
        assertThat(policy.currentPrice()).isPositive();
    }

    @Test
    @DisplayName("DOM_001: 수능이 지난 뒤에도 가격은 항상 양수")
    void afterSuneungPriceStaysPositive() {
        assertThat(policyAsOf(LocalDate.of(2026, 11, 20)).currentPrice()).isPositive();
    }
}
