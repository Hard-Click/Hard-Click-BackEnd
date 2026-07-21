package com.wanted.backend.domain.payment.infrastructure.persistence;

import com.wanted.backend.domain.payment.application.port.AdminPaymentQueryPort.AdminPaymentData;
import com.wanted.backend.domain.payment.domain.model.PaymentStatus;
import com.wanted.backend.domain.payment.domain.model.PaymentType;
import com.wanted.backend.domain.payment.infrastructure.member.PaymentMemberReferenceEntity;
import com.wanted.backend.domain.payment.infrastructure.member.PaymentMemberReferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 결제 목록 어댑터의 실제 쿼리 동작을 시딩 데이터로 검증한다.
 *
 * 핵심 회귀 방지: 실제 결제는 orders에만 기록되고 payments는 비어 있으므로, 관리자 목록이
 * orders를 원천으로 실 결제를 노출해야 한다(기존 payments 기반은 항상 0건이었다).
 * (ddl-auto=none + @EntityScan 스코프 + @Sql 최소 스키마로 두-엔티티 충돌/전체 DDL 생성을 피한다.)
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@EntityScan(basePackageClasses = {OrderJpaEntity.class, PaymentMemberReferenceEntity.class})
@EnableJpaRepositories(basePackageClasses = {OrderJpaRepository.class, PaymentMemberReferenceRepository.class})
@Import(AdminPaymentQueryAdapter.class)
@Sql(scripts = {"/sql/admin_payment_schema.sql", "/sql/admin_payment_data.sql"},
        config = @SqlConfig(encoding = "UTF-8"))
class AdminPaymentQueryAdapterTest {

    // 운영(AdminPaymentController)과 동일: paidAt DESC + 동률 안정화용 id DESC 보조 키
    private static final Pageable PAGE = PageRequest.of(0, 20,
            Sort.by(Sort.Direction.DESC, "paidAt").and(Sort.by(Sort.Direction.DESC, "id")));

    @Autowired
    private AdminPaymentQueryAdapter adapter;

    @Test
    @DisplayName("실 결제(orders)를 노출하고 미결제 READY는 제외하며 paidAt DESC + id DESC로 안정 정렬한다")
    void showsRealOrderBasedPaymentsExcludingReady() {
        Page<AdminPaymentData> page = adapter.search(null, null, PAGE);

        // 206(READY) 제외. 205와 207은 paidAt 동률(2026-07-18) → 보조 키 id DESC로 207이 205보다 앞.
        // 최종: 203(07-20) > 204(07-19) > 207(07-18,id↑) > 205(07-18) > 208(07-16)
        assertThat(page.getContent())
                .extracting(AdminPaymentData::paymentId)
                .containsExactly(203L, 204L, 207L, 205L, 208L);
    }

    @Test
    @DisplayName("리포트된 결제(203) 필드가 orders 기준으로 정확히 매핑된다")
    void mapsReportedPaymentFields() {
        AdminPaymentData p = adapter.search(null, "90BABF11", PAGE).getContent().get(0);

        assertThat(p.paymentId()).isEqualTo(203L);
        assertThat(p.orderId()).isEqualTo(203L);          // paymentId == orderId (/me와 동일)
        assertThat(p.orderNo()).isEqualTo("ORD-20260720-90BABF11");
        assertThat(p.paymentType()).isEqualTo(PaymentType.SUBSCRIPTION);
        assertThat(p.amount()).isEqualTo(3660000);
        assertThat(p.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.memberName()).isEqualTo("김근지");
        assertThat(p.memberEmail()).isEqualTo("geunji04@example.com");
    }

    @Test
    @DisplayName("참조 회원이 삭제돼도(members에 없음) 결제 행은 누락되지 않고 이름/이메일만 null")
    void deletedMemberReferenceStillAppears() {
        AdminPaymentData p = adapter.search(null, "DELMEM", PAGE).getContent().get(0);

        assertThat(p.paymentId()).isEqualTo(207L);
        assertThat(p.memberName()).isNull();
        assertThat(p.memberEmail()).isNull();
    }

    @Test
    @DisplayName("PAID 필터는 부분환불(PARTIAL_REFUNDED)까지 포함한다")
    void paidFilterIncludesPartialRefunded() {
        Page<AdminPaymentData> page = adapter.search(PaymentStatus.PAID, null, PAGE);

        assertThat(page.getContent())
                .extracting(AdminPaymentData::paymentId)
                .containsExactly(203L, 204L, 207L, 208L);
    }

    @Test
    @DisplayName("REFUNDED 필터는 환불 건만 반환한다")
    void refundedFilter() {
        Page<AdminPaymentData> page = adapter.search(PaymentStatus.REFUNDED, null, PAGE);

        assertThat(page.getContent())
                .extracting(AdminPaymentData::paymentId)
                .containsExactly(205L);
    }

    @Test
    @DisplayName("검색어는 주문번호와 회원 이름/이메일에 부분·대소문자무시로 매칭된다")
    void keywordSearchesOrderNoAndMember() {
        // 회원 이름(김근지 = member 100)의 노출 상태 주문: 203(PAID),205(REFUNDED),208(PARTIAL)
        assertThat(adapter.search(null, "김근지", PAGE).getContent())
                .extracting(AdminPaymentData::paymentId)
                .containsExactly(203L, 205L, 208L);

        // 이메일 부분검색(chulsoo = member 101): 204만(206 READY는 제외)
        assertThat(adapter.search(null, "CHULSOO", PAGE).getContent())
                .extracting(AdminPaymentData::paymentId)
                .containsExactly(204L);
    }
}
