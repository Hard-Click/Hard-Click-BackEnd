package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.order.application.port.OrderEnrollmentRevocationPort;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRefundOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PgClient pgClient;
    @Mock private OrderEnrollmentRevocationPort enrollmentRevocationPort;
    @Mock private CancelSubscriptionUseCase cancelSubscriptionUseCase;
    @Mock private DistributedLock distributedLock;

    @InjectMocks private AdminRefundOrderService service;

    private static final Long MEMBER_ID = 100L;

    @BeforeEach
    void setUp() {
        // 락을 획득한 것처럼 콜백을 그대로 실행한다.
        lenient().doAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return null;
        }).when(distributedLock).runWithLock(anyString(), any(Duration.class), any(Runnable.class));
    }

    private Order order(Long id, OrderType type, OrderStatus status, int finalAmount, List<OrderItem> items) {
        return Order.restore(id, "ORD-" + id, MEMBER_ID, type, status,
                finalAmount, finalAmount, LocalDateTime.now(), LocalDateTime.now(), "pk_" + id, items);
    }

    @Test
    @DisplayName("구독 주문은 실결제액 전액 PG 취소 후 구독을 취소한다")
    void refundsSubscriptionInFull() {
        Order order = order(203L, OrderType.SUBSCRIPTION, OrderStatus.PAID, 3660000, List.of());
        when(orderRepository.findById(203L)).thenReturn(Optional.of(order));

        service.refund(203L);

        verify(pgClient).cancel(eq("pk_203"), eq(3660000), anyString());
        verify(orderRepository).refundSubscription(203L);
        verify(cancelSubscriptionUseCase).handle(MEMBER_ID);
    }

    @Test
    @DisplayName("강의 다항목은 항목별로 PG 취소하고 마지막 항목에서 주문을 REFUNDED로 전이한다")
    void refundsAllCourseItems() {
        List<OrderItem> items = List.of(
                OrderItem.restore(1L, 11L, "강의A", 50000, false),
                OrderItem.restore(2L, 12L, "강의B", 39000, false));
        Order order = order(204L, OrderType.COURSE, OrderStatus.PAID, 89000, items);
        when(orderRepository.findById(204L)).thenReturn(Optional.of(order));

        service.refund(204L);

        verify(pgClient).cancel(eq("pk_204"), eq(50000), anyString());
        verify(pgClient).cancel(eq("pk_204"), eq(39000), anyString());
        // 첫 항목은 PARTIAL, 마지막 항목에서 REFUNDED
        verify(orderRepository).refundItem(204L, 11L, OrderStatus.PARTIAL_REFUNDED);
        verify(orderRepository).refundItem(204L, 12L, OrderStatus.REFUNDED);
        verify(enrollmentRevocationPort).revoke(MEMBER_ID, 11L);
        verify(enrollmentRevocationPort).revoke(MEMBER_ID, 12L);
    }

    @Test
    @DisplayName("이미 환불된 항목은 건너뛰고 남은 항목만 환불한다(부분환불 주문 재환불)")
    void skipsAlreadyRefundedItems() {
        List<OrderItem> items = List.of(
                OrderItem.restore(1L, 11L, "강의A", 50000, true),   // 이미 환불됨
                OrderItem.restore(2L, 12L, "강의B", 39000, false));
        Order order = order(205L, OrderType.COURSE, OrderStatus.PARTIAL_REFUNDED, 89000, items);
        when(orderRepository.findById(205L)).thenReturn(Optional.of(order));

        service.refund(205L);

        verify(pgClient, times(1)).cancel(eq("pk_205"), eq(39000), anyString());
        verify(orderRepository, never()).refundItem(eq(205L), eq(11L), any());
        verify(orderRepository).refundItem(205L, 12L, OrderStatus.REFUNDED);
        verify(enrollmentRevocationPort, never()).revoke(MEMBER_ID, 11L);
        verify(enrollmentRevocationPort).revoke(MEMBER_ID, 12L);
    }

    @Test
    @DisplayName("PAID/부분환불이 아닌 주문은 환불 불가(PG 미호출)")
    void rejectsNonRefundableStatus() {
        Order order = order(206L, OrderType.COURSE, OrderStatus.REFUNDED, 89000,
                List.of(OrderItem.restore(1L, 11L, "강의A", 89000, true)));
        when(orderRepository.findById(206L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.refund(206L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_REFUNDABLE);

        verifyNoInteractions(pgClient);
    }

    @Test
    @DisplayName("환불할 항목이 없으면(전부 환불됨) 환불 불가")
    void rejectsWhenNoUnrefundedItems() {
        Order order = order(207L, OrderType.COURSE, OrderStatus.PARTIAL_REFUNDED, 89000,
                List.of(OrderItem.restore(1L, 11L, "강의A", 89000, true)));
        when(orderRepository.findById(207L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.refund(207L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_REFUNDABLE);

        verifyNoInteractions(pgClient);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 ORDER_NOT_FOUND")
    void rejectsMissingOrder() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("락 획득 실패 시 중복 요청으로 처리하고 주문 조회조차 하지 않는다")
    void rejectsWhenLockNotAcquired() {
        doThrow(new BusinessException(ErrorCode.DUPLICATE_PAYMENT_REQUEST))
                .when(distributedLock).runWithLock(anyString(), any(Duration.class), any(Runnable.class));

        assertThatThrownBy(() -> service.refund(203L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_PAYMENT_REQUEST);

        verify(orderRepository, never()).findById(any());
    }

    @Test
    @DisplayName("PG 취소 실패는 PG_TIMEOUT으로 변환하고 DB 상태를 바꾸지 않는다")
    void mapsPgFailureToTimeout() {
        Order order = order(203L, OrderType.SUBSCRIPTION, OrderStatus.PAID, 3660000, List.of());
        when(orderRepository.findById(203L)).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("PG down")).when(pgClient).cancel(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.refund(203L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PG_TIMEOUT);

        verify(orderRepository, never()).refundSubscription(any());
    }
}
