package com.wanted.backend.domain.order.domain.policy;

import java.time.LocalDateTime;

/**
 * 강의(COURSE) 항목 환불 정책.
 * 프론트에 노출되는 규칙 "결제 후 7일 이내 + 진도율 10% 미만일 때만 환불 가능"의 단일 진실 소스.
 * refundable 계산(GetOrderService)과 환불 실행 재검증(RefundOrderItemService)이 동일 규칙을 공유한다.
 *
 * 구독(SUBSCRIPTION) 등 비강의 항목에는 적용하지 않는다(진도 개념이 없음).
 */
public final class OrderRefundPolicy {

    public static final int REFUND_WINDOW_DAYS = 7;
    /** 이 값 "이상" 진도면 환불 불가. 즉 10% 미만만 허용. */
    public static final int MAX_REFUNDABLE_PROGRESS_PERCENT = 10;

    private OrderRefundPolicy() {
    }

    public static boolean withinRefundWindow(LocalDateTime paidAt, LocalDateTime now) {
        if (paidAt == null) {
            return false;
        }
        return !now.isAfter(paidAt.plusDays(REFUND_WINDOW_DAYS));
    }

    public static boolean progressWithinLimit(int progressPercent) {
        return progressPercent < MAX_REFUNDABLE_PROGRESS_PERCENT;
    }

    public static boolean isCourseItemRefundable(LocalDateTime paidAt, LocalDateTime now, int progressPercent) {
        return withinRefundWindow(paidAt, now) && progressWithinLimit(progressPercent);
    }
}
