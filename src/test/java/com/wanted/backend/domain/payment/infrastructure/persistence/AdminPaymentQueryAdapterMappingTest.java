package com.wanted.backend.domain.payment.infrastructure.persistence;

import com.wanted.backend.domain.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 필터(PaymentStatus)를 실제 주문 상태 집합으로 변환하는 admin 전용 로직 검증.
 * (주문상태→PaymentStatus, 원문→PaymentType 매핑은 enum으로 이관 — PaymentStatusTest/PaymentTypeTest 참고.)
 */
class AdminPaymentQueryAdapterMappingTest {

    @Test
    @DisplayName("status 미지정 시 실결제 상태(PAID/PARTIAL_REFUNDED/REFUNDED/CANCELED)만 조회하고 미결제 READY는 제외한다")
    void resolveOrderStatuses_nullShowsVisibleOnly() {
        Set<String> statuses = AdminPaymentQueryAdapter.resolveOrderStatuses(null);

        assertThat(statuses).containsExactlyInAnyOrder("PAID", "PARTIAL_REFUNDED", "REFUNDED", "CANCELED");
        assertThat(statuses).doesNotContain("READY");
    }

    @Test
    @DisplayName("PAID 필터는 부분환불(PARTIAL_REFUNDED)까지 포함한다(주문상태→PaymentStatus 매핑 역적용)")
    void resolveOrderStatuses_paidIncludesPartialRefunded() {
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.PAID))
                .containsExactlyInAnyOrder("PAID", "PARTIAL_REFUNDED");
    }

    @Test
    @DisplayName("REFUNDED/CANCELED/READY 필터는 각각 해당 주문 상태로 매핑된다")
    void resolveOrderStatuses_singleMappings() {
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.REFUNDED)).containsExactly("REFUNDED");
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.CANCELED)).containsExactly("CANCELED");
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.READY)).containsExactly("READY");
    }

    @Test
    @DisplayName("orders에 대응 상태가 없는 필터(PENDING/FAILED)는 빈 집합 → 결과 없음")
    void resolveOrderStatuses_noOrderCounterpartIsEmpty() {
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.PENDING)).isEmpty();
        assertThat(AdminPaymentQueryAdapter.resolveOrderStatuses(PaymentStatus.FAILED)).isEmpty();
    }
}
