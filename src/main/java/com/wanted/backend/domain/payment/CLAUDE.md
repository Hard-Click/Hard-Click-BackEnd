# payment 도메인 규칙 (표준과 다른 위험만)

- **멱등성**: `payments.idempotency_key`(UNIQUE)로 중복 결제 방지. 결제 확정 로직 수정 시
  멱등성 키 검증을 절대 우회하지 말 것.
- **Toss 확정 검증**: amount / orderId / paymentKey 3개를 PG 응답과 대조 후 확정.
- **환불**: order ↔ payment 상태를 함께 동기화(RefundOrderItem / RefundPayment). 한쪽만 바꾸지 마.
- **주문 동시성**: 주문 조회/처리에 `PESSIMISTIC_WRITE` 락 사용(중복 결제 방지). 락 제거 금지.
