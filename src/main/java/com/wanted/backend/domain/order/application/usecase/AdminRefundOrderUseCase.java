package com.wanted.backend.domain.order.application.usecase;

/**
 * 관리자 오버라이드 환불. 주문 단위로 전액 환불하며, 학생 환불(RefundOrderItem/RefundSubscription)과 달리
 * 소유권 검증·환불 정책(기간/진도율)을 적용하지 않는다.
 */
public interface AdminRefundOrderUseCase {

    void refund(Long orderId);
}
