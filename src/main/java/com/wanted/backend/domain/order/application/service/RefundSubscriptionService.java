package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.order.application.port.OrderSubscriptionPlanPort;
import com.wanted.backend.domain.order.application.usecase.RefundSubscriptionUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundSubscriptionService implements RefundSubscriptionUseCase {

    private static final String CANCEL_REASON = "학생 요청에 의한 구독 환불";
    private static final String LOCK_KEY_PREFIX = "order:refund:lock:subscription:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final OrderRepository orderRepository;
    private final PgClient pgClient;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final OrderSubscriptionPlanPort subscriptionPlanPort;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void refund(Long memberId, Long orderId, String idempotencyKey) {
        String lockKey = LOCK_KEY_PREFIX + orderId;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (acquired == null || !acquired) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            if (!order.getMemberId().equals(memberId)) {
                throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
            }

            if (order.getType() != OrderType.SUBSCRIPTION) {
                throw new BusinessException(ErrorCode.ORDER_NOT_REFUNDABLE);
            }

            if (order.getStatus() != OrderStatus.PAID) {
                throw new BusinessException(ErrorCode.ORDER_NOT_REFUNDABLE);
            }

            // 환불 신청일 기준 일할 계산: 남은 D-day(수능까지) × 일일 단가 = 미사용분.
            // 결제 원금을 넘지 않도록 상한을 둔다(가격 정책 변동 대비).
            int refundAmount = Math.min(subscriptionPlanPort.getAnnualPass().price(), order.getFinalAmount());

            try {
                pgClient.cancel(order.getPaymentKey(), refundAmount, CANCEL_REASON);
            } catch (RuntimeException e) {
                throw new BusinessException(ErrorCode.PG_TIMEOUT, e);
            }

            orderRepository.refundSubscription(orderId);
            try {
                cancelSubscriptionUseCase.handle(memberId);
            } catch (RuntimeException e) {
                log.error("[SUBSCRIPTION_CANCEL_FAILED] DB 환불 완료됐지만 구독 취소 실패 — 수동 보정 필요. orderId: {}, memberId: {}", orderId, memberId, e);
            }

        } finally {
            try {
                redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
            } catch (RuntimeException e) {
                log.error("[SUBSCRIPTION_REFUND_LOCK_RELEASE_FAILED] orderId: {}, lockKey: {}", orderId, lockKey, e);
            }
        }
    }
}
