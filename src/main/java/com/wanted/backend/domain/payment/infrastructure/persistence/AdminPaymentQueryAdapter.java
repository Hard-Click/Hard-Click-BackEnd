package com.wanted.backend.domain.payment.infrastructure.persistence;

import com.wanted.backend.domain.payment.application.port.AdminPaymentQueryPort;
import com.wanted.backend.domain.payment.domain.model.PaymentStatus;
import com.wanted.backend.domain.payment.domain.model.PaymentType;
import com.wanted.backend.domain.payment.infrastructure.member.PaymentMemberReferenceEntity;
import com.wanted.backend.domain.payment.infrastructure.member.PaymentMemberReferenceRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 결제 목록 조회.
 *
 * 실제 결제 확정({@code ConfirmOrderPaymentService})은 {@code orders} 테이블만 PAID 처리하고
 * {@code payments} 행을 만들지 않는다(payments는 미사용 데모 경로 전용). 따라서 학생 본인 내역
 * ({@code MyPaymentHistoryQueryAdapter})과 동일하게 실제 결제 원천인 {@code orders}를 조회한다.
 * paymentId는 {@code /me}와 같게 orderId와 동일한 order.id를 사용한다(응답 스키마 불변).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentQueryAdapter implements AdminPaymentQueryPort {

    // 실제 결제로 취급하는 주문 상태(미결제 READY 등은 결제 관리 화면에서 제외). MyPaymentHistory와 동일.
    private static final Set<String> VISIBLE_STATUSES = Set.of(
            "PAID", "PARTIAL_REFUNDED", "REFUNDED", "CANCELED"
    );

    private final OrderJpaRepository orderRepository;
    private final PaymentMemberReferenceRepository memberRepository;

    @Override
    public Page<AdminPaymentData> search(PaymentStatus status, String keyword, Pageable pageable) {
        Set<String> targetStatuses = resolveOrderStatuses(status);
        if (targetStatuses.isEmpty()) {
            return Page.empty(pageable);
        }

        Specification<OrderJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in(targetStatuses));

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";

                Subquery<Long> memberSub = query.subquery(Long.class);
                var memberRoot = memberSub.from(PaymentMemberReferenceEntity.class);
                memberSub.select(memberRoot.get("id"))
                        .where(cb.or(
                                cb.like(cb.lower(memberRoot.get("name")), like),
                                cb.like(cb.lower(memberRoot.get("email")), like)
                        ));

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNo")), like),
                        root.get("memberId").in(memberSub)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OrderJpaEntity> orders = orderRepository.findAll(spec, pageable);
        if (orders.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> memberIds = orders.getContent().stream()
                .map(OrderJpaEntity::getMemberId)
                .distinct()
                .toList();
        Map<Long, PaymentMemberReferenceEntity> memberById = memberRepository.findByIdIn(memberIds).stream()
                .collect(Collectors.toMap(PaymentMemberReferenceEntity::getId, Function.identity()));

        List<AdminPaymentData> content = orders.getContent().stream()
                .map(order -> toData(order, memberById.get(order.getMemberId())))
                .toList();

        return new PageImpl<>(content, pageable, orders.getTotalElements());
    }

    private AdminPaymentData toData(OrderJpaEntity order, PaymentMemberReferenceEntity member) {
        return new AdminPaymentData(
                order.getId(),
                order.getId(),
                order.getOrderNo(),
                PaymentType.fromRawOrDefault(order.getPaymentType()),
                member == null ? null : member.getName(),
                member == null ? null : member.getEmail(),
                order.getFinalAmount(),
                PaymentStatus.fromOrderStatus(order.getStatus()),
                order.getPaidAt()
        );
    }

    // 관리자 필터(PaymentStatus)를 실제 주문 상태 문자열 집합으로 변환한다.
    // MyPaymentHistory의 주문상태→PaymentStatus 매핑(PAID/PARTIAL_REFUNDED→PAID 등)을 역으로 적용한다.
    // 단위 테스트가 직접 검증하도록 package-private static.
    static Set<String> resolveOrderStatuses(PaymentStatus status) {
        if (status == null) {
            return VISIBLE_STATUSES;
        }
        return switch (status) {
            case PAID -> Set.of("PAID", "PARTIAL_REFUNDED");
            case REFUNDED -> Set.of("REFUNDED");
            case CANCELED -> Set.of("CANCELED");
            case READY -> Set.of("READY");
            // orders에 대응 상태가 없는 필터(PENDING/FAILED)는 결과 없음.
            case PENDING, FAILED -> Set.of();
        };
    }
}
