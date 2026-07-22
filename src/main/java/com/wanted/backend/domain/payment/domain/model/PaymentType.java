package com.wanted.backend.domain.payment.domain.model;

public enum PaymentType {
    COURSE,
    SUBSCRIPTION;

    /** 주문의 payment_type 문자열을 결제 타입으로 변환한다. null·미지원 값은 COURSE로 폴백. */
    public static PaymentType fromRawOrDefault(String raw) {
        if (raw == null) {
            return COURSE;
        }
        try {
            return PaymentType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return COURSE;
        }
    }
}
