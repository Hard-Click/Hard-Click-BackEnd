package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.order.application.dto.OrderDetailResult;
import com.wanted.backend.domain.order.application.port.OrderCourseProgressPort;
import com.wanted.backend.domain.order.application.port.OrderCourseQueryPort;
import com.wanted.backend.domain.order.application.port.OrderEnrollmentStatusPort;
import com.wanted.backend.domain.order.application.port.OrderSubscriptionPlanPort;
import com.wanted.backend.domain.order.application.usecase.GetOrderUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
import com.wanted.backend.domain.order.domain.model.OrderItem;
import com.wanted.backend.domain.order.domain.model.OrderStatus;
import com.wanted.backend.domain.order.domain.model.OrderType;
import com.wanted.backend.domain.order.domain.policy.OrderRefundPolicy;
import com.wanted.backend.domain.order.domain.repository.OrderRepository;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEnrollmentStatusPort orderEnrollmentStatusPort;
    private final OrderCourseQueryPort orderCourseQueryPort;
    private final OrderSubscriptionPlanPort orderSubscriptionPlanPort;
    private final OrderCourseProgressPort orderCourseProgressPort;
    private final Clock clock;

    @Override
    public OrderDetailResult getOrder(Long memberId, Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // SUBSCRIPTION 주문은 order_items에 행이 없으므로 플랜 정보로 합성
        List<OrderItem> rawItems = order.getItems();
        if (order.getType() == OrderType.SUBSCRIPTION && rawItems.isEmpty()) {
            OrderSubscriptionPlanPort.PlanInfo plan = orderSubscriptionPlanPort.getAnnualPass();
            rawItems = List.of(OrderItem.create(null, plan.name(), order.getTotalAmount()));
        }

        List<Long> courseIds = rawItems.stream()
                .map(OrderItem::getCourseId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, String> enrollStatuses = courseIds.isEmpty()
                ? Map.of()
                : orderEnrollmentStatusPort.findEnrollStatuses(memberId, courseIds);

        Map<Long, String> thumbnailByCourseId = courseIds.isEmpty()
                ? Map.of()
                : orderCourseQueryPort.findAllByIds(courseIds).stream()
                        .collect(Collectors.toMap(OrderCourseQueryPort.CourseInfo::courseId,
                                info -> info.thumbnailUrl() != null ? info.thumbnailUrl() : "",
                                (a, b) -> a));

        boolean orderPaid =
                order.getStatus() == OrderStatus.PAID
                        || order.getStatus() == OrderStatus.PARTIAL_REFUNDED;

        // 강의 항목 환불 정책(7일 이내 + 진도율 10% 미만) 판정을 위한 진도율 조회
        Map<Long, Integer> progressByCourseId = courseIds.isEmpty()
                ? Map.of()
                : orderCourseProgressPort.findProgressPercents(memberId, courseIds);
        LocalDateTime now = LocalDateTime.now(clock);

        // 구독 예상 환불액 = 실제 환불식(RefundSubscriptionService)과 동일해야 함.
        // 신청일 기준 남은 일수 × 일일 단가(=현재가)와 결제액 중 작은 값.
        int subscriptionRefundAmount = order.getType() == OrderType.SUBSCRIPTION
                ? Math.min(orderSubscriptionPlanPort.getAnnualPass().price(), order.getFinalAmount())
                : 0;

        List<OrderDetailResult.Item> items = rawItems.stream()
                .map(item -> {

                    boolean refundable;
                    if (item.getCourseId() == null) {
                        // 구독 등 비강의 항목: 7일/진도율 규칙 미적용(진도 개념 없음)
                        refundable = orderPaid && !item.isRefunded();
                    } else {
                        int progressPercent = progressByCourseId.getOrDefault(item.getCourseId(), 0);
                        refundable = orderPaid && !item.isRefunded()
                                && OrderRefundPolicy.isCourseItemRefundable(order.getPaidAt(), now, progressPercent);
                    }

                    int refundAmount;
                    if (item.isRefunded()) {
                        refundAmount = 0;
                    } else if (order.getType() == OrderType.SUBSCRIPTION && item.getCourseId() == null) {
                        // 구독 항목: 전액(price)이 아니라 실제 환불되는 비례액을 노출
                        refundAmount = subscriptionRefundAmount;
                    } else {
                        refundAmount = item.getPrice();
                    }

                    String enrollStatus =
                            item.getCourseId() == null
                                    ? null
                                    : enrollStatuses.getOrDefault(item.getCourseId(), "NONE");

                    String thumbnailUrl =
                            item.getCourseId() == null
                                    ? null
                                    : thumbnailByCourseId.getOrDefault(item.getCourseId(), null);

                    return new OrderDetailResult.Item(
                            item.getCourseId(),
                            item.getTitle(),
                            thumbnailUrl,
                            item.getPrice(),
                            refundable,
                            refundAmount,
                            item.isRefunded(),
                            enrollStatus
                    );
                })
                .toList();

        return new OrderDetailResult(
                order.getOrderNo(),
                order.getStatus(),
                order.getType(),
                order.getOrderedAt(),
                order.getPaidAt(),
                items,
                order.getTotalAmount()
        );
    }
}