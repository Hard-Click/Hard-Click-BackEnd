package com.wanted.backend.domain.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTypeTest {

    @Test
    @DisplayName("원문 문자열을 결제 타입으로 변환한다")
    void fromRawOrDefault_mapping() {
        assertThat(PaymentType.fromRawOrDefault("SUBSCRIPTION")).isEqualTo(PaymentType.SUBSCRIPTION);
        assertThat(PaymentType.fromRawOrDefault("COURSE")).isEqualTo(PaymentType.COURSE);
    }

    @Test
    @DisplayName("null·미지원 값은 COURSE로 폴백한다")
    void fromRawOrDefault_fallback() {
        assertThat(PaymentType.fromRawOrDefault(null)).isEqualTo(PaymentType.COURSE);
        assertThat(PaymentType.fromRawOrDefault("UNKNOWN")).isEqualTo(PaymentType.COURSE);
    }
}
