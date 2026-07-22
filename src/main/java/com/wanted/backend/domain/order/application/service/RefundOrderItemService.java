package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.order.application.port.OrderCourseProgressPort;
import com.wanted.backend.domain.order.application.port.OrderEnrollmentRevocationPort;
import com.wanted.backend.domain.order.application.usecase.RefundOrderItemUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
import com.wanted.backend.domain.order.domain.model.OrderItem;
import com.wanted.backend.domain.order.domain.model.OrderStatus;
import com.wanted.backend.domain.order.domain.policy.OrderRefundPolicy;
import com.wanted.backend.domain.order.domain.repository.OrderRepository;
import com.wanted.backend.domain.payment.application.port.PgClient;
import com.wanted.backend.global.common.DistributedLock;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 항목 단위 환불. 실제 Toss 결제취소(/v1/payments/{paymentKey}/cancel) 호출 후
 * 수강 권한 박탈 + 주문/항목 상태 갱신을 처리한다.
 *
 * 락: order:refund:lock:{orderId}:{courseId} (TTL 30초, SETNX) — 동일 주문 항목에 대한
 * 동시 환불 요청을 직렬화한다. PG 취소는 @Transactional 밖에서 실행해 DB 커넥션을 점유하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundOrderItemService implements RefundOrderItemUseCase {

    private static final String CANCEL_REASON = "학생 요청에 의한 강의 환불";
    private static final String LOCK_KEY_PREFIX = "order:refund:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final OrderRepository orderRepository;
    private final OrderEnrollmentRevocationPort enrollmentRevocationPort;
    private final OrderCourseProgressPort orderCourseProgressPort;
    private final PgClient pgClient;
    private final DistributedLock distributedLock;
    private final Clock clock;

    @Override
    public void refund(Long memberId, Long orderId, Long courseId, String idempotencyKey) {
        // 동일 주문 항목에 대한 동시 환불 방지
        distributedLock.runWithLock(LOCK_KEY_PREFIX + orderId + ":" + courseId, LOCK_TTL, () -> {
            // Step 1: 검증 (짧은 읽기 전용 TX)
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

            if (!order.getMemberId().equals(memberId)) {
                throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
            }

            if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PARTIAL_REFUNDED) {
                throw new BusinessException(ErrorCode.ORDER_NOT_REFUNDABLE);
            }

            OrderItem item = order.getItems().stream()
                    .filter(i -> courseId.equals(i.getCourseId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

            if (item.isRefunded()) {
                return;
            }

            // 환불 정책 재검증(서버 강제): 프론트 문구(7일 이내 + 진도율 10% 미만)만으론 우회 가능하므로
            // 실제 환불 실행 시점에 다시 확인한다. GetOrderService.refundable과 동일 규칙(OrderRefundPolicy)을 공유.
            LocalDateTime now = LocalDateTime.now(clock);
            if (!OrderRefundPolicy.withinRefundWindow(order.getPaidAt(), now)) {
                throw new BusinessException(ErrorCode.REFUND_WINDOW_EXPIRED);
            }
            int progressPercent = orderCourseProgressPort
                    .findProgressPercents(memberId, List.of(courseId))
                    .getOrDefault(courseId, 0);
            if (!OrderRefundPolicy.progressWithinLimit(progressPercent)) {
                throw new BusinessException(ErrorCode.REFUND_PROGRESS_EXCEEDED);
            }

            boolean allOthersAlreadyRefunded = order.getItems().stream()
                    .filter(i -> !courseId.equals(i.getCourseId()))
                    .allMatch(OrderItem::isRefunded);
            OrderStatus newStatus = allOthersAlreadyRefunded ? OrderStatus.REFUNDED : OrderStatus.PARTIAL_REFUNDED;

            // Step 2: PG 취소 — @Transactional 밖에서 실행 (DB 커넥션 미점유)
            // PG 취소 성공 후 DB 업데이트 실패 시 운영자 수동 보정 대상(ERROR 로그)
            try {
                pgClient.cancel(order.getPaymentKey(), item.getPrice(), CANCEL_REASON);
            } catch (RuntimeException e) {
                throw new BusinessException(ErrorCode.PG_TIMEOUT, e);
            }

            // Step 3: DB 상태 갱신 (orderRepository.refundItem 자체 @Transactional)
            orderRepository.refundItem(orderId, courseId, newStatus);
            try {
                enrollmentRevocationPort.revoke(memberId, courseId);
            } catch (RuntimeException e) {
                log.error("[REFUND_REVOKE_FAILED] DB 환불 완료됐지만 수강권 박탈 실패 — 수동 보정 필요. orderId: {}, courseId: {}", orderId, courseId, e);
            }
        });
    }
}
