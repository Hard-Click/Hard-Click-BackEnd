package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.enrollment_management.application.command.EnrollCommand;
import com.wanted.backend.domain.enrollment_management.application.usecase.EnrollUseCase;
import com.wanted.backend.domain.order.application.port.OrderCartDeletePort;
import com.wanted.backend.domain.order.application.usecase.ConfirmOrderPaymentUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
import com.wanted.backend.domain.order.domain.model.OrderItem;
import com.wanted.backend.domain.order.domain.model.OrderStatus;
import com.wanted.backend.domain.order.domain.model.OrderType;
import com.wanted.backend.domain.order.domain.repository.OrderRepository;
import com.wanted.backend.domain.payment.application.port.PgClient;
import com.wanted.backend.domain.subscription.application.usecase.SubscribeUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * checkout으로 만들어진 READY 주문을 실제 PG 승인과 묶어 확정하고,
 * 주문 타입에 따라 수강권(COURSE) 또는 구독권(SUBSCRIPTION)을 지급한다.
 *
 * 락: order:pay:lock:{idempotencyKey} (TTL 30초, SETNX)
 * 멱등: order.status가 READY가 아니면(이미 PAID 등) PG 재호출 없이 즉시 멱등 응답.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmOrderPaymentService implements ConfirmOrderPaymentUseCase {

    private static final String LOCK_KEY_PREFIX = "order:pay:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final OrderRepository orderRepository;
    private final PgClient pgClient;
    private final EnrollUseCase enrollUseCase;
    private final SubscribeUseCase subscribeUseCase;
    private final OrderCartDeletePort orderCartDeletePort;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Override
    public Result confirm(Long memberId, String orderNo, String paymentKey, Integer amount, String idempotencyKey) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "FAILED";
        try {
            Result result = doConfirm(memberId, orderNo, paymentKey, amount, idempotencyKey);
            outcome = result.duplicate() ? "DUPLICATE" : "SUCCESS";
            return result;
        } catch (BusinessException e) {
            outcome = e.getErrorCode() == ErrorCode.DUPLICATE_PAYMENT_REQUEST ? "DUPLICATE" : "FAILED";
            throw e;
        } finally {
            Counter.builder("order.payment.result").tag("status", outcome).register(meterRegistry).increment();
            sample.stop(Timer.builder("order.payment.processing.duration").register(meterRegistry));
        }
    }

    private Result doConfirm(Long memberId, String orderNo, String paymentKey, Integer amount, String idempotencyKey) {
        Order order = findOwnedOrder(memberId, orderNo);

        // 이미 처리된 주문이면 PG 재호출 없이 즉시 멱등 응답
        if (order.getStatus() != OrderStatus.READY) {
            return new Result(orderNo, order.getStatus(), null, true);
        }

        if (order.getFinalAmount() != amount) {
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }

        String lockKey = LOCK_KEY_PREFIX + idempotencyKey;
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, LOCK_TTL);
        if (acquired == null || !acquired) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST);
        }

        try {
            // 락 대기 중 다른 스레드가 이미 처리를 끝냈을 수 있음
            Order raced = findOwnedOrder(memberId, orderNo);
            if (raced.getStatus() != OrderStatus.READY) {
                return new Result(orderNo, raced.getStatus(), null, true);
            }

            String pgTransactionId;
            try {
                pgTransactionId = pgClient.confirm(paymentKey, orderNo, amount);
            } catch (RuntimeException e) {
                throw new BusinessException(ErrorCode.PG_TIMEOUT, e);
            }

            LocalDateTime paidAt = LocalDateTime.now(clock);
            orderRepository.markPaid(orderNo, paidAt, pgTransactionId);

            dispatchAccessGrant(raced);

            return new Result(orderNo, OrderStatus.PAID, pgTransactionId, false);
        } finally {
            try {
                releaseLockSafely(lockKey, lockValue);
            } catch (RuntimeException e) {
                log.error("[PAYMENT_LOCK_RELEASE_FAILED] orderNo: {}, lockKey: {}", orderNo, lockKey, e);
            }
        }
    }

    private Order findOwnedOrder(Long memberId, String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    // 결제 자체는 이미 PG에서 확정됐으므로, 수강권/구독권 지급 실패는 주문을 되돌리지 않고
    // 운영 알람용 ERROR 로그만 남긴다(수동 보정 대상).
    private void dispatchAccessGrant(Order order) {
        try {
            if (order.getType() == OrderType.SUBSCRIPTION) {
                subscribeUseCase.handle(order.getMemberId(), order.getId(), order.getFinalAmount());
                orderCartDeletePort.deleteAllByMemberId(order.getMemberId());
            } else {
                List<Long> purchasedCourseIds = order.getItems().stream()
                        .map(OrderItem::getCourseId)
                        .filter(java.util.Objects::nonNull)
                        .toList();
                for (OrderItem item : order.getItems()) {
                    if (item.getCourseId() != null) {
                        grantEnrollment(order.getMemberId(), item.getCourseId());
                    }
                }
                orderCartDeletePort.deleteByMemberIdAndCourseIds(order.getMemberId(), purchasedCourseIds);
            }
        } catch (RuntimeException e) {
            // 결제(과금)는 이미 확정됐으나 지급만 실패한 상태 — 무음으로 삼키면 사용자는 과금 후 아무것도
            // 못 받는다. 수동 보정 대상임을 알람으로 표면화하기 위해 실패 메트릭을 남긴다.
            meterRegistry.counter("order.payment.access_grant.failed",
                    "type", order.getType().name()).increment();
            log.error("[ACCESS_GRANT_FAILED] 결제는 완료됐지만 수강권/구독권 지급 실패 — orderNo: {}, type: {}",
                    order.getOrderNo(), order.getType(), e);
        }
    }

    private void grantEnrollment(Long memberId, Long courseId) {
        try {
            enrollUseCase.handle(new EnrollCommand(memberId, courseId));
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.ENROLLMENT_ALREADY_EXISTS) {
                throw e;
            }
        }
    }

    private void releaseLockSafely(String lockKey, String lockValue) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), lockValue);
    }
}
