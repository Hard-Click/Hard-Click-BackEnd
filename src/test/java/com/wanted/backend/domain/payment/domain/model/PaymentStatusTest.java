package com.wanted.backend.domain.payment.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @Test
    @DisplayName("주문 상태를 결제 상태로 매핑한다(PAID/PARTIAL_REFUNDED→PAID)")
    void fromOrderStatus_mapping() {
        assertThat(PaymentStatus.fromOrderStatus("PAID")).isEqualTo(PaymentStatus.PAID);
        assertThat(PaymentStatus.fromOrderStatus("PARTIAL_REFUNDED")).isEqualTo(PaymentStatus.PAID);
        assertThat(PaymentStatus.fromOrderStatus("REFUNDED")).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(PaymentStatus.fromOrderStatus("CANCELED")).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    @DisplayName("미매핑 주문 상태(READY 등)는 READY로 폴백한다")
    void fromOrderStatus_defaultReady() {
        assertThat(PaymentStatus.fromOrderStatus("READY")).isEqualTo(PaymentStatus.READY);
        assertThat(PaymentStatus.fromOrderStatus("SOMETHING_ELSE")).isEqualTo(PaymentStatus.READY);
    }
}
