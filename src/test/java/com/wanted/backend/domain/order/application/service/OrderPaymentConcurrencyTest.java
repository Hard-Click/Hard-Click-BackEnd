package com.wanted.backend.domain.order.application.service;

import com.wanted.backend.domain.enrollment_management.application.usecase.EnrollUseCase;
import com.wanted.backend.domain.order.application.port.OrderCartDeletePort;
import com.wanted.backend.domain.order.application.usecase.ConfirmOrderPaymentUseCase;
import com.wanted.backend.domain.order.domain.model.Order;
import com.wanted.backend.domain.order.domain.model.OrderStatus;
import com.wanted.backend.domain.order.domain.model.OrderType;
import com.wanted.backend.domain.order.domain.repository.OrderRepository;
import com.wanted.backend.domain.payment.application.port.PgClient;
import com.wanted.backend.domain.subscription.application.usecase.SubscribeUseCase;
import com.wanted.backend.global.exception.BusinessException;
import com.wanted.backend.global.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 결제 확정 경로의 동시성/멱등성 검증.
 *
 * 시나리오: 동일 orderNo + 동일 Idempotency-Key로 50개 스레드가 동시에 결제 확정을 호출한다.
 * 기대: PG confirm은 정확히 1회, DB markPaid도 정확히 1회만 실행되고, 나머지는 중복(멱등) 처리된다.
 *
 * Redis 락(SETNX)은 ConcurrentHashMap.putIfAbsent 로, 락 해제(Lua)는 map remove 로 흉내 낸다.
 */
class OrderPaymentConcurrencyTest {

    private static final Long MEMBER_ID = 100L;
    private static final String ORDER_NO = "ORD-1";
    private static final int AMOUNT = 10_000;

    @Test
    @DisplayName("동일 멱등키 50개 동시 결제 확정 시 PG confirm과 markPaid는 각각 1회만 실행된다")
    void confirmIsIdempotentUnderConcurrency() throws InterruptedException {
        // given
        int threadCount = 50;

        FakeOrderRepository orderRepository = new FakeOrderRepository(
                Order.restore(1L, ORDER_NO, MEMBER_ID, OrderType.COURSE, OrderStatus.READY,
                        AMOUNT, AMOUNT, LocalDateTime.now(), LocalDateTime.now(), "pk_1", List.of()));

        AtomicInteger pgConfirmCount = new AtomicInteger();
        PgClient pgClient = new PgClient() {
            @Override
            public String confirm(String paymentKey, String orderId, Integer amount) {
                pgConfirmCount.incrementAndGet();
                return "pg-tx-" + orderId;
            }

            @Override
            public void cancel(String paymentKey, Integer cancelAmount, String cancelReason) {
            }
        };

        StringRedisTemplate redisTemplate = fakeRedisTemplate();

        ConfirmOrderPaymentService service = new ConfirmOrderPaymentService(
                orderRepository,
                pgClient,
                mock(EnrollUseCase.class),
                mock(SubscribeUseCase.class),
                mock(OrderCartDeletePort.class),
                redisTemplate,
                new SimpleMeterRegistry(),
                Clock.systemUTC());

        String idempotencyKey = "11111111-1111-1111-1111-111111111111";

        // when: 50개 스레드가 동시에 같은 결제를 확정 시도
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger duplicate = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    ConfirmOrderPaymentUseCase.Result result =
                            service.confirm(MEMBER_ID, ORDER_NO, "pk_1", AMOUNT, idempotencyKey);
                    if (result.duplicate()) {
                        duplicate.incrementAndGet();
                    } else {
                        success.incrementAndGet();
                    }
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.DUPLICATE_PAYMENT_REQUEST) {
                        duplicate.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown(); // 동시 출발
        pool.shutdown();
        boolean finished = pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        // then
        assertThat(finished).isTrue();
        assertThat(pgConfirmCount.get()).as("PG confirm 호출 횟수").isEqualTo(1);
        assertThat(orderRepository.markPaidCount.get()).as("DB markPaid 횟수").isEqualTo(1);
        assertThat(success.get()).as("실제 결제 확정(비중복) 응답 수").isEqualTo(1);
        assertThat(success.get() + duplicate.get()).as("성공+중복 처리 = 전체 요청").isEqualTo(threadCount);
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate fakeRedisTemplate() {
        ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenAnswer(inv -> store.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenAnswer(inv -> {
                    List<String> keys = inv.getArgument(1);
                    store.remove(keys.get(0));
                    return 1L;
                });
        return redisTemplate;
    }

    /** 주문 상태 전이를 스레드 세이프하게 기록하는 인메모리 fake. */
    private static class FakeOrderRepository implements OrderRepository {
        private final AtomicReference<Order> ref;
        final AtomicInteger markPaidCount = new AtomicInteger();

        FakeOrderRepository(Order initial) {
            this.ref = new AtomicReference<>(initial);
        }

        @Override
        public synchronized Optional<Order> findByOrderNo(String orderNo) {
            return Optional.of(ref.get());
        }

        @Override
        public synchronized void markPaid(String orderNo, LocalDateTime paidAt, String paymentKey) {
            markPaidCount.incrementAndGet();
            Order cur = ref.get();
            ref.set(Order.restore(cur.getId(), cur.getOrderNo(), cur.getMemberId(), cur.getType(),
                    OrderStatus.PAID, cur.getFinalAmount(), cur.getFinalAmount(),
                    LocalDateTime.now(), LocalDateTime.now(), paymentKey, cur.getItems()));
        }

        @Override public Order save(Order order) { throw new UnsupportedOperationException(); }
        @Override public Optional<Order> findById(Long orderId) { throw new UnsupportedOperationException(); }
        @Override public Optional<Order> findByIdForUpdate(Long orderId) { throw new UnsupportedOperationException(); }
        @Override public void refundItem(Long orderId, Long courseId, OrderStatus newOrderStatus) { throw new UnsupportedOperationException(); }
        @Override public void refundSubscription(Long orderId) { throw new UnsupportedOperationException(); }
    }
}
