package com.wanted.backend.domain.payment.infrastructure.persistence;

import com.wanted.backend.domain.payment.domain.model.PaymentStatus;
import com.wanted.backend.domain.payment.domain.model.PaymentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 결제 목록의 상태/타입 매핑 검증.
 * DB 없이 검증 가능한 순수 로직만 다룬다(orders 쿼리 자체는 @SpringBootTest 통합 테스트 영역).
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

    @Test
    @DisplayName("주문 상태를 응답용 PaymentStatus로 매핑한다(PAID/PARTIAL_REFUNDED→PAID)")
    void toPaymentStatus_mapping() {
        assertThat(AdminPaymentQueryAdapter.toPaymentStatus("PAID")).isEqualTo(PaymentStatus.PAID);
        assertThat(AdminPaymentQueryAdapter.toPaymentStatus("PARTIAL_REFUNDED")).isEqualTo(PaymentStatus.PAID);
        assertThat(AdminPaymentQueryAdapter.toPaymentStatus("REFUNDED")).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(AdminPaymentQueryAdapter.toPaymentStatus("CANCELED")).isEqualTo(PaymentStatus.CANCELED);
        assertThat(AdminPaymentQueryAdapter.toPaymentStatus("READY")).isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("결제 타입은 문자열을 enum으로 변환하고, null·미지원 값은 COURSE로 폴백한다")
    void toPaymentType_mapping() {
        assertThat(AdminPaymentQueryAdapter.toPaymentType("SUBSCRIPTION")).isEqualTo(PaymentType.SUBSCRIPTION);
        assertThat(AdminPaymentQueryAdapter.toPaymentType("COURSE")).isEqualTo(PaymentType.COURSE);
        assertThat(AdminPaymentQueryAdapter.toPaymentType(null)).isEqualTo(PaymentType.COURSE);
        assertThat(AdminPaymentQueryAdapter.toPaymentType("UNKNOWN")).isEqualTo(PaymentType.COURSE);
    }
}
