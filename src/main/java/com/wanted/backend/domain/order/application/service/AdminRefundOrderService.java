package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.order.application.port.OrderEnrollmentRevocationPort;
import com.wanted.backend.domain.order.application.usecase.AdminRefundOrderUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
import com.wanted.backend.domain.order.domain.model.OrderItem;
import com.wanted.backend.domain.order.domain.model.OrderStatus;
import com.wanted.backend.domain.order.domain.model.OrderType;
import com.wanted.backend.domain.order.domain.repository.OrderRepository;
import com.wanted.backend.domain.payment.application.port.PgClient;
import com.wanted.backend.domain.subscription.application.usecase.CancelSubscriptionUseCase;
import com.wanted.backend.global.common.DistributedLock;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 관리자 결제 환불. 학생 환불과 달리 소유권·환불 정책 게이트가 없는 관리자 오버라이드다.
 * 주문 타입에 따라 강의(미환불 항목 전부) 또는 구독을 전액 환불한다.
 *
 * 동시성: 공통 {@link DistributedLock}(orderId 키)으로 동일 주문 동시 환불을 직렬화하고,
 * PG 취소는 락 콜백 안에서 실행하되 트랜잭션 밖이다(DB 커넥션 미점유). 강의 다항목은 항목별로
 * PG 취소 + DB 상태 갱신을 묶어(재시도 시 이미 환불된 항목은 건너뜀) 부분 실패 후 재시도가 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRefundOrderService implements AdminRefundOrderUseCase {

    private static final String CANCEL_REASON = "관리자 환불 처리";
    private static final String LOCK_KEY_PREFIX = "order:refund:lock:admin:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final OrderRepository orderRepository;
    private final PgClient pgClient;
    private final OrderEnrollmentRevocationPort enrollmentRevocationPort;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final DistributedLock distributedLock;

    @Override
    public void refund(Long orderId) {
        distributedLock.runWithLock(LOCK_KEY_PREFIX + orderId, LOCK_TTL, () -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
                throw new BusinessException(ErrorCode.ORDER_NOT_REFUNDABLE);
            }

            if (order.getType() == OrderType.SUBSCRIPTION) {
                refundSubscription(order);
            } else {
                refundCourseOrder(order);
            }
        });
    }

    // 관리자 오버라이드: 실결제액(finalAmount) 전액 환불.
    private void refundSubscription(Order order) {
        cancelPg(order.getPaymentKey(), order.getFinalAmount());
        orderRepository.refundSubscription(order.getId());
        try {
            cancelSubscriptionUseCase.handle(order.getMemberId());
        } catch (RuntimeException e) {
            log.error("[ADMIN_REFUND_SUBSCRIPTION_CANCEL_FAILED] DB 환불 완료됐지만 구독 취소 실패 — 수동 보정 필요. orderId: {}",
                    order.getId(), e);
        }
    }

    private void refundCourseOrder(Order order) {
        List<OrderItem> unrefunded = order.getItems().stream()
                .filter(item -> !item.isRefunded() && item.getCourseId() != null)
                .toList();
        if (unrefunded.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_NOT_REFUNDABLE);
        }

        // 항목별로 PG 취소 + DB 갱신을 함께 처리해 부분 실패 후 재시도가 안전하다(이미 환불된 항목은 위 필터에서 제외).
        for (int i = 0; i < unrefunded.size(); i++) {
            OrderItem item = unrefunded.get(i);
            OrderStatus newStatus = (i == unrefunded.size() - 1) ? OrderStatus.REFUNDED : OrderStatus.PARTIAL_REFUNDED;

            cancelPg(order.getPaymentKey(), item.getPrice());
            orderRepository.refundItem(order.getId(), item.getCourseId(), newStatus);
            try {
                enrollmentRevocationPort.revoke(order.getMemberId(), item.getCourseId());
            } catch (RuntimeException e) {
                log.error("[ADMIN_REFUND_REVOKE_FAILED] DB 환불 완료됐지만 수강권 박탈 실패 — 수동 보정 필요. orderId: {}, courseId: {}",
                        order.getId(), item.getCourseId(), e);
            }
        }
    }

    private void cancelPg(String paymentKey, int amount) {
        try {
            pgClient.cancel(paymentKey, amount, CANCEL_REASON);
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.PG_TIMEOUT, e);
        }
    }
}
