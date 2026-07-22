package com.wanted.backend.domain.payment.domain.model;

public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED,
    READY,
    FAILED,
    CANCELED;

    public static PaymentStatus from(String value) {
        return PaymentStatus.valueOf(value);
    }

    /**
     * 주문(order) 상태 문자열을 결제 상태로 매핑한다.
     * PAID·부분환불(PARTIAL_REFUNDED)은 PAID, 환불/취소는 각각, 그 외(미결제 READY 등)는 READY.
     */
    public static PaymentStatus fromOrderStatus(String orderStatus) {
        return switch (orderStatus) {
            case "PAID", "PARTIAL_REFUNDED" -> PAID;
            case "REFUNDED" -> REFUNDED;
            case "CANCELED" -> CANCELED;
            default -> READY;
        };
    }
}
