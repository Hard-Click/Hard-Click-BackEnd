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
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 결제 환불. 학생 환불과 달리 소유권·환불 정책 게이트가 없는 관리자 오버라이드다.
 * 주문 타입에 따라 강의(미환불 항목 전부) 또는 구독을 전액 환불한다.
 *
 * 학생 환불 서비스와 동일한 패턴: Redis 락(orderId)으로 동일 주문 동시 환불을 직렬화하고,
 * PG 취소는 @Transactional 밖에서 실행해 DB 커넥션을 점유하지 않는다. 강의 다항목은 항목별로
 * PG 취소 + DB 상태 갱신을 묶어(재시도 시 이미 환불된 항목은 건너뜀) 부분 실패 후 재시도가 안전하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRefundOrderService implements AdminRefundOrderUseCase {

    private static final String CANCEL_REASON = "관리자 환불 처리";
    private static final String LOCK_KEY_PREFIX = "order:refund:lock:admin:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final OrderRepository orderRepository;
    private final PgClient pgClient;
    private final OrderEnrollmentRevocationPort enrollmentRevocationPort;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void refund(Long orderId) {
        String lockKey = LOCK_KEY_PREFIX + orderId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (acquired == null || !acquired) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        try {
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
        } finally {
            try {
                redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
            } catch (RuntimeException e) {
                log.error("[ADMIN_REFUND_LOCK_RELEASE_FAILED] orderId: {}", orderId, e);
            }
        }
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
